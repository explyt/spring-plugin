/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.RouterBuilderRouteWalker
import com.explyt.spring.web.route.RouterDslRoutes
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType

internal sealed class FunctionalRouteTarget(val call: UCallExpression, val factory: PsiMethod) {

    class Handler(call: UCallExpression, factory: PsiMethod, val method: PsiMethod) :
        FunctionalRouteTarget(call, factory)

    class Lambda(call: UCallExpression, factory: PsiMethod, val lambda: ULambdaExpression) :
        FunctionalRouteTarget(call, factory)

    val start: PsiMethod
        get() = when (this) {
            is Handler -> method
            is Lambda -> factory
        }

    val startBody: UElement?
        get() = (this as? Lambda)?.lambda?.body

    val requestMethods: List<String>?
        get() = RouterDslRoutes.requestMethodsOf(call)?.let { SpringWebUtil.getServedRequestMethods(call, it) }

    val paths: List<String>
        get() = walkedBuilderPaths().ifEmpty { declaredPaths() }

    val route: String?
        get() {
            val method = requestMethods?.let(McpHttpMethods::routeVerb) ?: return null
            val path = paths.firstOrNull() ?: return null
            return "$method $path"
        }

    private fun declaredPaths(): List<String> =
        if (isNestedUnderUnresolvedPrefix()) emptyList()
        else SpringWebUtil.getPathsFromCallExpression(call).filter { it.isNotEmpty() }

    private fun walkedBuilderPaths(): List<String> {
        val routeCall = call.sourcePsi as? PsiMethodCallExpression ?: return emptyList()
        return RouterBuilderRouteWalker.walkOf(factory).routes.filter { it.call == routeCall }.map { it.path }
    }

    private fun isNestedUnderUnresolvedPrefix(): Boolean {
        var argument: UElement = call
        var node = call.uastParent
        while (node != null && node !is UMethod) {
            ProgressManager.checkCanceled()
            if (node is UCallExpression && node.takesArgument(argument) && node.hidesPrefix()) return true
            if (node is ULambdaExpression) argument = node
            node = node.uastParent
        }
        return false
    }

    private fun UCallExpression.takesArgument(argument: UElement): Boolean =
        valueArguments.any { it.sourcePsi != null && it.sourcePsi == argument.sourcePsi }

    private fun UCallExpression.hidesPrefix(): Boolean =
        if (lang.id == KotlinLanguage.INSTANCE.id) {
            SpringWebUtil.isNestCall(this) && SpringWebUtil.getNestPrefixesOrNullIfUnresolved(this) == null
        } else {
            methodName in JAVA_NESTING_CALLS
        }

    companion object {

        private val JAVA_NESTING_CALLS = setOf("nest", "path")

        private val ROUTER_FUNCTIONS = listOf(SpringWebClasses.ROUTE_FUNCTION, SpringWebClasses.SERVLET_ROUTE_FUNCTION)

        fun atLine(psiFile: PsiFile, document: Document, line: Int): FunctionalRouteTarget? {
            val lineIndex = (line - 1).coerceIn(0, document.lineCount - 1)
            val lineEnd = document.getLineEndOffset(lineIndex)
            var offset = document.getLineStartOffset(lineIndex)

            while (offset <= lineEnd) {
                ProgressManager.checkCanceled()
                val leaf = psiFile.findElementAt(offset) ?: return null
                registeredAround(leaf)?.let { return it }
                offset = maxOf(leaf.textRange.endOffset, offset + 1)
            }
            return null
        }

        fun of(routeElement: PsiElement): FunctionalRouteTarget? =
            routeElement.toUElement()?.getParentOfType<UCallExpression>(strict = false)?.let(::fromCall)

        private fun registeredAround(leaf: PsiElement): FunctionalRouteTarget? =
            generateSequence(leaf) { it.parent }
                .takeWhile { it !is PsiFile }
                .mapNotNull { it.toUElementOfType<UCallExpression>() }
                .filter { leaf.textRange.startOffset >= nameStartOf(it) }
                .firstNotNullOfOrNull(::fromCall)

        private fun isRoute(call: UCallExpression): Boolean =
            call.methodName in SpringWebClasses.ROUTER_DSL_ROUTE_METHODS || RouterDslRoutes.requestMethodsOf(call) != null

        private fun nameStartOf(call: UCallExpression): Int =
            (call.methodIdentifier?.sourcePsi ?: call.sourcePsi)?.textRange?.startOffset ?: Int.MAX_VALUE

        private fun fromCall(call: UCallExpression): FunctionalRouteTarget? {
            if (!isRoute(call)) return null
            val factory = call.getParentOfType<UMethod>()?.javaPsi ?: return null
            val returnType = factory.returnType ?: return null
            if (ROUTER_FUNCTIONS.none { returnType.isInheritorOf(it) }) return null
            return when (val handler = call.valueArguments.lastOrNull()) {
                is UCallableReferenceExpression -> (handler.resolve() as? PsiMethod)?.let { Handler(call, factory, it) }
                is ULambdaExpression -> Lambda(call, factory, handler)
                else -> null
            }
        }
    }
}
