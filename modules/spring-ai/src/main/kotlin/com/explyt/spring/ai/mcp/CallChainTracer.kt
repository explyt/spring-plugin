/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USuperExpression
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * The project methods a method reaches, in the order `explyt_trace_spring_call_chain` reports them.
 *
 * Only project sources are traced. A call into the JDK, the Kotlin standard library or a framework jar names nothing
 * a caller can change, and following every resolvable call buried the few project methods under `Instant.now` and
 * `trim`, spent the depth on them, and listed every test calling `Instant.now` as a test the trace would break. The
 * one framework call kept is a member called on a project type - `repository.findById` inherited from `CrudRepository`
 * - because the repository layer of a Spring Data application consists of nothing else; it is listed, not traced.
 *
 * Depth counts calls into other classes. A private helper, a supertype, a companion or a top-level function of the
 * same file is still the same unit of code, and charging a level for it stopped a controller-to-repository trace
 * inside the controller. Methods are expanded nearest first, so a method reached both through a helper and through
 * a longer path is expanded with the depth the shorter one leaves.
 *
 * A call written against an interface or an abstract method reaches whatever implements it, which is where the
 * behaviour is: it is followed to every implementation in the project. A constructor call creates a value rather
 * than passing a request on, and is left out.
 */
internal class CallChainTracer(project: Project, private val maxMethods: Int) {

    private val fileIndex = ProjectFileIndex.getInstance(project)
    private val projectScope = GlobalSearchScope.projectScope(project)

    fun trace(start: PsiMethod, depth: Int): CallChain {
        val traced = LinkedHashMap<String, TracedMethod>()
        val pending = ArrayDeque<Pending>().apply { add(Pending(start, depth)) }

        while (pending.isNotEmpty() && traced.size < maxMethods) {
            val (method, remainingDepth) = pending.removeFirst()
            val key = methodKey(method)
            if (key in traced) continue
            val calls = callsOf(method)
            traced[key] = TracedMethod(method, calls)

            val reached = calls.mapNotNull { call -> call.reached?.let { it to call.internal } }
            reached.filter { (_, internal) -> internal }.asReversed()
                .forEach { (callee, _) -> pending.addFirst(Pending(callee, remainingDepth)) }
            if (remainingDepth > 1) {
                reached.filterNot { (_, internal) -> internal }
                    .forEach { (callee, _) -> pending.addLast(Pending(callee, remainingDepth - 1)) }
            }
        }
        return CallChain(traced.values.toList(), truncated = pending.any { methodKey(it.method) !in traced })
    }

    private data class Pending(val method: PsiMethod, val remainingDepth: Int)

    private fun callsOf(method: PsiMethod): List<TracedCall> {
        val uMethod = method.toUElement() as? UMethod ?: return emptyList()
        val calls = mutableListOf<TracedCall>()
        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                ProgressManager.checkCanceled()
                calls += callsAt(node, method)
                return false
            }
        })
        return calls.distinctBy { Triple(it.target, it.via, it.reached?.let(::methodKey)) }
    }

    private fun callsAt(call: UCallExpression, caller: PsiMethod): List<TracedCall> {
        val callee = call.resolve() ?: return emptyList()
        if (callee.isConstructor || methodKey(callee) == methodKey(caller)) return emptyList()
        val line = (call.methodIdentifier?.sourcePsi ?: call.sourcePsi)?.let(McpSourcePositions::lineOfAnchor)

        if (!isProjectSource(callee)) {
            return listOfNotNull(frameworkMemberOfProjectType(call, callee, line))
        }
        val implementations = if (callee.hasModifierProperty(PsiModifier.ABSTRACT)) implementationsOf(callee) else emptyList()
        if (implementations.isEmpty()) {
            return listOf(TracedCall(nameOf(callee), line, callee, via = null, internal = isInternal(caller, callee)))
        }
        return implementations.map {
            TracedCall(nameOf(it), line, it, via = nameOf(callee), internal = isInternal(caller, it))
        }
    }

    /**
     * A framework interface method called on a project type, named after that type: `DemoRepository.findById`, not
     * `CrudRepository.findById` - the project type is what the caller recognises and can open.
     *
     * Only interface methods qualify: that is the shape of a Spring Data repository, while a member inherited from a
     * class - `Object.toString`, `Enum.name`, a getter of a library base entity - says nothing about the project.
     */
    private fun frameworkMemberOfProjectType(call: UCallExpression, callee: PsiMethod, line: Int?): TracedCall? {
        if (callee.hasModifierProperty(PsiModifier.STATIC) || callee.containingClass?.isInterface != true) return null
        val receiver = call.receiver ?: return null
        if (receiver is UThisExpression || receiver is USuperExpression) return null
        val receiverClass = PsiUtil.resolveClassInClassTypeOnly(call.receiverType) ?: return null
        if (!isProjectSource(receiverClass)) return null
        return TracedCall("${receiverClass.name}.${callee.name}", line, reached = null, via = null, internal = false)
    }

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

    private fun isProjectSource(element: PsiElement): Boolean {
        val file = element.navigationElement.containingFile?.virtualFile ?: return false
        return fileIndex.isInSourceContent(file)
    }

    private fun nameOf(method: PsiMethod): String = "${method.containingClass?.name ?: "?"}.${sourceNameOf(method)}"

    companion object {

        /**
         * The name a function is declared with, not the one the JVM sees: a Kotlin `internal` function compiles to
         * `activitySql$module_name`, a name that appears nowhere in the source a caller would search.
         */
        fun sourceNameOf(method: PsiMethod): String =
            ((method as? KtLightMethod)?.kotlinOrigin as? KtNamedFunction)?.name ?: method.name
    }
}

/**
 * The methods a trace reached, in the order it expanded them, the method it started from first.
 *
 * @property truncated whether reachable methods were left out because the trace hit its size limit.
 */
internal class CallChain(val methods: List<TracedMethod>, val truncated: Boolean) {

    private val ids = methods.withIndex().associate { (index, traced) -> methodKey(traced.method) to index }

    /** Position of [method] in [methods], or `null` when the trace did not expand it. */
    fun idOf(method: PsiMethod): Int? = ids[methodKey(method)]
}

internal class TracedMethod(val method: PsiMethod, val calls: List<TracedCall>)

/**
 * One call a traced method makes.
 *
 * @property line the line of the call itself, where a change to its arguments is made.
 * @property reached the project method the call reaches, which the trace follows; `null` for a framework method.
 * @property via the declaration the call is written against when it reaches an implementation of it.
 * @property internal whether the call stays in the caller's own unit of code and so costs no depth.
 */
internal data class TracedCall(
    val target: String,
    val line: Int?,
    val reached: PsiMethod?,
    val via: String?,
    val internal: Boolean,
)

/**
 * Identity of a method across the PSI instances resolving to it: a Kotlin light method is re-created by every
 * resolve, so neither identity nor `equals` recognises a method already traced.
 */
private fun methodKey(method: PsiMethod): String =
    "${method.containingClass?.qualifiedName}#${method.name}" +
            method.parameterList.parameters.joinToString(prefix = "(", postfix = ")") { it.type.canonicalText }
