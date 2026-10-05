/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.InheritanceUtil

import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * The project methods a method reaches, in the order `explyt_trace_spring_call_chain` reports them.
 *
 * Only methods with a body in project sources are traced. A call into the JDK, the Kotlin standard library or a
 * framework jar names nothing a caller can change, and following every resolvable call buried the few project methods
 * under `Instant.now` and `trim`, spent the depth on them, and listed every test calling `Instant.now` as a test the
 * trace would break.
 *
 * A call where the request leaves the application is kept as an [CallKind.EXTERNAL] leaf, because that is where the
 * I/O happens: a library method called on an injected dependency - `jdbcTemplate.query`, `kafkaTemplate.send` - and a
 * method the framework implements for a project interface - a Spring Data repository method, declared or inherited
 * from `CrudRepository`, or an HTTP client interface method. The two repository shapes are reported alike, since
 * neither has a body the trace could read.
 *
 * Depth counts calls into other classes. A private helper, a supertype, a companion or a top-level function of the
 * same file is still the same unit of code, and charging a level for it stopped a controller-to-repository trace
 * inside the controller. Methods are expanded nearest first, so a method reached both through a helper and through
 * a longer path is expanded with the depth the shorter one leaves.
 *
 * A call written against an interface or an abstract method reaches whatever implements it, which is where the
 * behaviour is: it is followed to every implementation in the project. A constructor call creates a value rather
 * than passing a request on, and is left out.
 *
 * A method passed as a callable reference - `input.use(validator::validate)`, `orders.forEach(repository::save)` - is
 * invoked by the function it is passed to, so it is a call of the method that passes it ([MethodCallSite]).
 *
 * A call the IDE cannot resolve is kept when it is made on an injected dependency ([UnresolvedCallSite]): the jar the
 * build declares may not be downloaded yet, or the method may be a typo, and dropping the call left the method looking
 * like one that calls nothing. It is listed as an [CallKind.EXTERNAL] leaf named after the declared type of the
 * dependency and marked unresolved, since nothing can be followed, even when that type is a project class. A call on
 * anything else - a fresh object, a type, the result of an earlier call in a fluent chain, or a call with an implicit
 * receiver inside `with(dsl)` or `dsl.apply` - names no dependency the trace could attribute it to, and stays out.
 */
internal class CallChainTracer(project: Project, private val maxMethods: Int) {

    private val projectScope = GlobalSearchScope.projectScope(project)

    fun trace(start: PsiMethod, depth: Int): CallChain {
        val traced = LinkedHashMap<String, TracedMethod>()
        val pending = ArrayDeque<Pending>().apply { add(Pending(start, depth, reachedBy = null)) }

        while (pending.isNotEmpty() && traced.size < maxMethods) {
            val (method, remainingDepth, reachedBy) = pending.removeFirst()
            val key = methodKey(method)
            if (key in traced) continue
            val calls = callsOf(method)
            traced[key] = TracedMethod(method, reachedBy, calls)

            calls.filter { it.kind == CallKind.INTERNAL }.asReversed()
                .forEach { pending.addFirst(Pending(it.reached!!, remainingDepth, CallKind.INTERNAL)) }
            if (remainingDepth > 1) {
                calls.filter { it.kind == CallKind.PROJECT }
                    .forEach { pending.addLast(Pending(it.reached!!, remainingDepth - 1, CallKind.PROJECT)) }
            }
        }
        return CallChain(traced.values.toList(), truncated = pending.any { methodKey(it.method) !in traced })
    }

    private data class Pending(val method: PsiMethod, val remainingDepth: Int, val reachedBy: CallKind?)

    private fun callsOf(method: PsiMethod): List<TracedCall> {
        val uMethod = method.toUElement() as? UMethod ?: return emptyList()
        val calls = mutableListOf<TracedCall>()
        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                ProgressManager.checkCanceled()
                CallSite.of(node)?.let { calls += callsAt(it, method) }
                return false
            }

            override fun visitCallableReferenceExpression(node: UCallableReferenceExpression): Boolean {
                ProgressManager.checkCanceled()
                CallSite.of(node)?.let { calls += callsAt(it, method) }
                return false
            }
        })
        return calls.distinctBy { listOf(it.target, it.via, it.reached?.let(::methodKey), it.resolved) }
    }

    private fun callsAt(site: CallSite, caller: PsiMethod): List<TracedCall> = when (site) {
        is MethodCallSite -> callsAt(site, caller)
        is UnresolvedCallSite -> listOfNotNull(unresolvedCallOnInjectedDependency(site, caller))
    }

    private fun unresolvedCallOnInjectedDependency(site: UnresolvedCallSite, caller: PsiMethod): TracedCall? {
        val owner = caller.containingClass ?: return null
        val field = InjectedDependencies.fieldOf(site.receiver, owner) ?: return null
        return TracedCall(
            "${InjectedDependencies.declaredTypeNameOf(field)}.${site.methodName}",
            site.line, reached = null, viaMethod = null, CallKind.EXTERNAL, resolved = false,
        )
    }

    private fun callsAt(site: MethodCallSite, caller: PsiMethod): List<TracedCall> {
        val callee = site.callee
        if (callee.isConstructor || methodKey(callee) == methodKey(caller)) return emptyList()
        val line = site.line

        if (!isProjectSource(callee)) {
            return listOfNotNull(
                frameworkMemberOfProjectType(site, line) ?: injectedDependencyCall(site, caller, line)
            )
        }
        if (!callee.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return listOf(projectCall(line, callee, viaMethod = null, caller))
        }
        val implementations = implementationsOf(callee)
        if (implementations.isEmpty()) {
            return listOf(TracedCall(nameOf(callee), line, reached = null, viaMethod = null, CallKind.EXTERNAL))
        }
        return implementations.map { projectCall(line, it, viaMethod = callee, caller) }
    }

    private fun projectCall(line: Int?, callee: PsiMethod, viaMethod: PsiMethod?, caller: PsiMethod) = TracedCall(
        nameOf(callee), line, callee, viaMethod,
        if (isInternal(caller, callee)) CallKind.INTERNAL else CallKind.PROJECT,
    )

    /**
     * A framework interface method called on a project type, named after that type: `DemoRepository.findById`, not
     * `CrudRepository.findById` - the project type is what the caller recognises and can open.
     *
     * Only interface methods qualify: that is the shape of a Spring Data repository, while a member inherited from a
     * class - `Object.toString`, `Enum.name`, a getter of a library base entity - says nothing about the project.
     */
    private fun frameworkMemberOfProjectType(site: MethodCallSite, line: Int?): TracedCall? {
        val callee = site.callee
        if (callee.hasModifierProperty(PsiModifier.STATIC) || callee.containingClass?.isInterface != true) return null
        if (site.receiver == null || site.isOnSelf) return null
        val receiverClass = site.receiverClass ?: return null
        if (!isProjectSource(receiverClass)) return null
        return externalCall(receiverClass, callee, line)
    }

    /**
     * A library method called on a dependency the container injected into the caller's class, named after the declared
     * type of the dependency: `JdbcTemplate.query`. JDK and Kotlin standard library members are left out - an
     * injected `Clock` or a `List` of handlers is plumbing, not a point where the request leaves the application.
     */
    private fun injectedDependencyCall(site: MethodCallSite, caller: PsiMethod, line: Int?): TracedCall? {
        val callee = site.callee
        val calleeClass = callee.containingClass?.qualifiedName ?: return null
        if (PLATFORM_PACKAGES.any(calleeClass::startsWith)) return null
        val owner = caller.containingClass ?: return null
        InjectedDependencies.fieldOf(site.receiver, owner) ?: return null
        val receiverClass = site.receiverClass ?: return null
        return externalCall(receiverClass, callee, line)
    }

    private fun externalCall(receiverClass: PsiClass, callee: PsiMethod, line: Int?) =
        TracedCall("${receiverClass.name}.${callee.name}", line, reached = null, viaMethod = null, CallKind.EXTERNAL)

    private fun implementationsOf(method: PsiMethod): List<PsiMethod> =
        OverridingMethodsSearch.search(method, projectScope, true).findAll()
            .filter { !it.hasModifierProperty(PsiModifier.ABSTRACT) && isProjectSource(it) }
            .sortedBy { methodKey(it) }

    /**
     * Whether [callee] belongs to the same unit of code as [caller], so calling it costs no depth: the caller's own
     * class, a supertype of it, a class enclosing it, a class it encloses such as a companion, or a top-level
     * function of its file. Two classes nested in one outer class are separate units - the outer class is often
     * no more than a namespace.
     */
    private fun isInternal(caller: PsiMethod, callee: PsiMethod): Boolean {
        val callerClass = caller.containingClass ?: return false
        val calleeClass = callee.containingClass ?: return false
        return callerClass.enclosingClasses().any { InheritanceUtil.isInheritorOrSelf(it, calleeClass, true) }
                || calleeClass.enclosingClasses().any { InheritanceUtil.isInheritorOrSelf(callerClass, it, true) }
                || isTopLevelFunctionOfTheFileOf(callee, caller)
    }

    private fun PsiClass.enclosingClasses(): Sequence<PsiClass> = generateSequence(this, PsiClass::getContainingClass)

    private fun isTopLevelFunctionOfTheFileOf(callee: PsiMethod, caller: PsiMethod): Boolean {
        val declaration = (callee as? KtLightMethod)?.kotlinOrigin ?: return false
        return declaration.parent is KtFile && declaration.containingFile == caller.navigationElement.containingFile
    }

    private fun isProjectSource(element: PsiElement): Boolean = ProjectSources.declares(element)

    companion object {

        /** How a method is named in a trace: its declaring class and its source name, `ShortLinkService.activity`. */
        fun nameOf(method: PsiMethod): String = "${method.containingClass?.name ?: "?"}.${sourceNameOf(method)}"

        /**
         * The name a function is declared with, not the one the JVM sees: a Kotlin `internal` function compiles to
         * `activitySql$module_name`, a name that appears nowhere in the source a caller would search.
         */
        fun sourceNameOf(method: PsiMethod): String = kotlinFunctionOf(method)?.name ?: method.name

        /**
         * The parameters a method is declared with. The JVM signature of a Kotlin function carries more: the receiver of
         * an extension function as `$this$name`, and the continuation of a `suspend` function as `$completion`.
         */
        fun sourceParametersOf(method: PsiMethod): List<String> =
            kotlinFunctionOf(method)?.valueParameters?.mapNotNull { it.name }
                ?: method.parameterList.parameters.map { it.name }

        private fun kotlinFunctionOf(method: PsiMethod): KtNamedFunction? =
            (method as? KtLightMethod)?.kotlinOrigin as? KtNamedFunction

        private val PLATFORM_PACKAGES = listOf("java.", "kotlin.")
    }
}

/** What a call made by a traced method reaches. */
internal enum class CallKind {
    /** A project method of the caller's own unit of code - a helper - traced without spending depth. */
    INTERNAL,

    /** A project method of another class, traced while depth remains. */
    PROJECT,

    /** A point where the request leaves the application, listed and not traced. */
    EXTERNAL,
}

/**
 * The methods a trace reached, in the order it expanded them, the method it started from first.
 *
 * @property truncated whether reachable methods were left out because the trace hit its size limit.
 */
internal class CallChain(val methods: List<TracedMethod>, val truncated: Boolean) {

    private val ids = methods.withIndex().associate { (index, traced) -> methodKey(traced.method) to index }

    private val viaDeclarations: Map<String, List<PsiMethod>> = methods
        .flatMap { it.calls }
        .mapNotNull { call -> call.reached?.let { reached -> call.viaMethod?.let { methodKey(reached) to it } } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, declarations) -> declarations.distinctBy(::methodKey) }

    /** Position of [method] in [methods], or `null` when the trace did not expand it. */
    fun idOf(method: PsiMethod): Int? = ids[methodKey(method)]

    /**
     * The interface or abstract declarations the trace reached [method] through. Code written against such a
     * declaration - a caller, a mock - depends on [method] without naming it.
     */
    fun viaDeclarationsOf(method: PsiMethod): List<PsiMethod> = viaDeclarations[methodKey(method)].orEmpty()
}

/**
 * @property reachedBy the kind of call the method was first reached through, or `null` for the method the trace
 *   started from.
 */
internal class TracedMethod(val method: PsiMethod, val reachedBy: CallKind?, val calls: List<TracedCall>)

/**
 * One call a traced method makes.
 *
 * @property line the line of the call itself, where a change to its arguments is made.
 * @property reached the project method the call reaches, which the trace follows; `null` for an external call.
 * @property viaMethod the declaration the call is written against when it reaches an implementation of it.
 * @property resolved `false` when the IDE cannot resolve the method, so [target] is read from the call site and the
 *   declared type of the dependency it is made on rather than from the callee.
 */
internal data class TracedCall(
    val target: String,
    val line: Int?,
    val reached: PsiMethod?,
    val viaMethod: PsiMethod?,
    val kind: CallKind,
    val resolved: Boolean = true,
) {
    val via: String? get() = viaMethod?.let(CallChainTracer::nameOf)
}

/**
 * Identity of a method across the PSI instances resolving to it: a Kotlin light method is re-created by every
 * resolve, so neither identity nor `equals` recognises a method already traced.
 */
private fun methodKey(method: PsiMethod): String =
    "${method.containingClass?.qualifiedName}#${method.name}" +
            method.parameterList.parameters.joinToString(prefix = "(", postfix = ")") { it.type.canonicalText }
