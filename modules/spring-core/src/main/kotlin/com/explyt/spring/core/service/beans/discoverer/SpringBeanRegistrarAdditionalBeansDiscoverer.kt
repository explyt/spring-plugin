/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */
package com.explyt.spring.core.service.beans.discoverer

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchUtils
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass

/**
 * Discover beans registered programmatically via Spring 7 BeanRegistrar/BeanRegistrarDsl.
 * Heuristic parser: extracts bean classes from registry.registerBean(X.class), registerBean<T>(),
 * and registerBean { Baz(...) } supplier-lambda constructs.
 */
class SpringBeanRegistrarAdditionalBeansDiscoverer : AdditionalBeansDiscoverer() {

    override fun accepts(module: Module): Boolean {
        // Enabled only if BeanRegistrar (or Kotlin DSL) exists on classpath
        return beanRegistrarClass(module) != null || beanRegistrarDslClass(module) != null
    }

    override fun discoverBeans(module: Module): Collection<PsiBean> {
        val result = mutableSetOf<PsiBean>()
        val registrarInterface = beanRegistrarClass(module)
        if (registrarInterface != null) {
            SpringSearchUtils.searchClassInheritors(registrarInterface).forEach { impl ->
                parseJavaRegistrar(module, impl).let { result.addAll(it) }
            }
        }
        val registrarDsl = beanRegistrarDslClass(module)
        if (registrarDsl != null) {
            SpringSearchUtils.searchClassInheritors(registrarDsl).forEach { impl ->
                parseKotlinRegistrarDsl(module, impl).let { result.addAll(it) }
            }
        }
        return result
    }

    private fun parseJavaRegistrar(module: Module, psiClass: PsiClass): Collection<PsiBean> {
        val uClass = psiClass.toUElementOfType<UClass>() ?: return emptyList()
        val beans = mutableSetOf<PsiBean>()
        val methods = uClass.methods.filter { it.name == "register" }
        methods.forEach { m ->
            m.uastBody?.accept(object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node.methodName != "registerBean") return super.visitCallExpression(node)
                    // Try to resolve bean class from class literal argument
                    val classFromArg =
                        node.valueArguments.firstNotNullOfOrNull { (it as? UClassLiteralExpression)?.type?.resolvedPsiClass }
                    val classFromTypeArg = if (node.typeArgumentCount > 0) {
                        node.typeArguments.firstOrNull()?.resolvedPsiClass
                    } else null
                    val target: PsiClass? = classFromArg ?: classFromTypeArg
                    if (target != null) {
                        val explicitName = (node.valueArguments.firstOrNull() as? ULiteralExpression)
                            ?.takeIf { it.value is String }
                            ?.value as? String
                        if (!explicitName.isNullOrBlank()) {
                            beans += PsiBean(name = explicitName, psiClass = target, psiMember = target)
                        } else {
                            beans += PsiBean(target)
                        }
                    }
                    return super.visitCallExpression(node)
                }
            })
        }
        return beans
    }

    private fun parseKotlinRegistrarDsl(module: Module, psiClass: PsiClass): Collection<PsiBean> {
        val beans = mutableSetOf<PsiBean>()
        val fileU = psiClass.containingFile?.toUElementOfType<UFile>()
        fileU?.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val callName = node.methodName ?: node.resolve()?.name
                if (callName != "registerBean") return super.visitCallExpression(node)

                // 1) Generic: registerBean<T>()
                var typeArgTarget = node.typeArguments.firstOrNull()?.resolvedPsiClass
                if (typeArgTarget == null) {
                    // Heuristic: parse source text for registerBean<SomeType>() when UAST doesn't expose type args for Kotlin reified
                    val src = node.sourcePsi?.text
                    if (src != null) {
                        val m = Regex("registerBean<([A-Za-z0-9_.]+)").find(src)
                        val shortOrFqn = m?.groupValues?.getOrNull(1)
                        if (!shortOrFqn.isNullOrBlank()) {
                            val pkg = psiClass.qualifiedName?.substringBeforeLast('.', "") ?: ""
                            val fqnCandidate = if (shortOrFqn.contains('.')) shortOrFqn else listOf(pkg, shortOrFqn).filter { it.isNotBlank() }.joinToString(".")
                            typeArgTarget = findClass(module, fqnCandidate)
                                ?: JavaPsiFacade.getInstance(module.project)
                                    .findClass(shortOrFqn, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
                        }
                    }
                }
                if (typeArgTarget != null) {
                    addBeanFromNode(beans, node, typeArgTarget)
                    return super.visitCallExpression(node)
                }

                // 2) Supplier lambda: registerBean { Baz(...) }
                val lambda = node.valueArguments.firstNotNullOfOrNull {
                    (it as? ULambdaExpression) ?: (it as? UNamedExpression)?.expression as? ULambdaExpression
                }
                if (lambda != null) {
                    // Find a constructor call in the lambda body (e.g., registerBean { Baz(...) })
                    val candidateClass = when (val body = lambda.body) {
                        is UBlockExpression -> body.expressions.asSequence().mapNotNull { extractConstructedClass(it) }.firstOrNull()
                        else -> extractConstructedClass(body)
                    }
                    if (candidateClass != null) {
                        addBeanFromNode(beans, node, candidateClass)
                    }
                }
                return super.visitCallExpression(node)
            }
        })

        // Fallback: pure text parsing in registrar file if UAST was not available or missed matches
        if (beans.isEmpty()) {
            val registrarFileText = psiClass.containingFile?.text ?: ""
            if (registrarFileText.isNotBlank()) {
                // registerBean<T>() generic
                Regex("registerBean<([A-Za-z0-9_.]+)>").findAll(registrarFileText).forEach { m ->
                    val typeName = m.groupValues[1]
                    val target = resolveTypeByName(module, psiClass, typeName) ?: return@forEach
                    beans += PsiBean(target)
                }
                // supplier: registerBean { Baz(...) }
                Regex("registerBean\\s*\\{\\s*([A-Za-z0-9_.]+)\\s*\\(").findAll(registrarFileText).forEach { m ->
                    val typeName = m.groupValues[1]
                    val target = resolveTypeByName(module, psiClass, typeName) ?: return@forEach
                    beans += PsiBean(target)
                }
            }
        }
        return beans
    }

    private fun extractConstructedClass(expr: UExpression): PsiClass? = when (expr) {
        is UCallExpression -> {
            val fromResolve = expr.resolve()?.containingClass
            fromResolve ?: (expr.classReference?.resolve() as? PsiClass)
        }
        is UQualifiedReferenceExpression -> extractConstructedClass(expr.selector)
        is UParenthesizedExpression -> extractConstructedClass(expr.expression)
        is UReturnExpression -> expr.returnExpression?.let { extractConstructedClass(it) }
        else -> null
    }

    private fun addBeanFromNode(collector: MutableSet<PsiBean>, call: UCallExpression, target: PsiClass) {

        val explicitName = call.valueArguments
            .mapNotNull { it as? UNamedExpression }
            .firstOrNull { it.name == "name" }
            ?.expression
            ?.let { (it as? ULiteralExpression)?.value as? String }
        if (!explicitName.isNullOrBlank()) {
            collector += PsiBean(name = explicitName, psiClass = target, psiMember = target)
        } else {
            collector += PsiBean(target)
        }
    }

    private fun resolveTypeByName(module: Module, contextClass: PsiClass, shortOrFqn: String): PsiClass? {
        if (shortOrFqn.contains('.')) {
            return findClass(module, shortOrFqn)
        }
        val pkg = contextClass.qualifiedName?.substringBeforeLast('.', "") ?: ""
        val fqnCandidate = if (pkg.isBlank()) shortOrFqn else "$pkg.$shortOrFqn"
        return findClass(module, fqnCandidate) ?: findClass(module, shortOrFqn)
    }

    private fun beanRegistrarClass(module: Module): PsiClass? =
        findClass(module, BEAN_REGISTRAR)

    private fun beanRegistrarDslClass(module: Module): PsiClass? =
        findClass(module, BEAN_REGISTRAR_DSL)

    private fun findClass(module: Module, fqn: String): PsiClass? {
        return LibraryClassCache.searchForLibraryClass(module, fqn)
            ?: JavaPsiFacade.getInstance(module.project)
                .findClass(fqn, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
    }

    companion object {
        private const val BEAN_REGISTRAR = "org.springframework.beans.factory.BeanRegistrar"
        private const val BEAN_REGISTRAR_DSL = "org.springframework.beans.factory.dsl.BeanRegistrarDsl"
    }
}

