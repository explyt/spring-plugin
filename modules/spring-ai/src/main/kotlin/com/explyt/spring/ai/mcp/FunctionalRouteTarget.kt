/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.RoutePathResolver
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
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

    val verb: String?
        get() = SpringWebUtil.getRequestMethod(call)

    val paths: List<String>
        get() = if (isNestedUnderUnresolvedPrefix()) emptyList()
        else SpringWebUtil.getPathsFromCallExpression(call).filter { it.isNotEmpty() }

    val route: String?
        get() {
            val verb = verb ?: return null
            val path = paths.firstOrNull() ?: return null
            return "$verb $path"
        }

    private fun isNestedUnderUnresolvedPrefix(): Boolean {
        var argument: UElement = call
        var node = call.uastParent
        while (node != null && node !is UMethod) {
            ProgressManager.checkCanceled()
            if (node is UCallExpression && node.methodName in NESTING_CALLS && node.takesArgument(argument)) {
                if (!hasResolvedPrefix(node)) return true
            }
            if (node is ULambdaExpression) argument = node
            node = node.uastParent
        }
        return false
    }

    private fun UCallExpression.takesArgument(argument: UElement): Boolean =
        valueArguments.any { it.sourcePsi != null && it.sourcePsi == argument.sourcePsi }

    private fun hasResolvedPrefix(nesting: UCallExpression): Boolean {
        if (nesting.lang.id != KotlinLanguage.INSTANCE.id) return false
        val receiver = nesting.receiver ?: return false
        if (receiver is UCallExpression && receiver.methodName == PATH_CALL) return false
        val isPathText = receiver.getExpressionType()?.canonicalText in STRING_TYPES
        return !isPathText || RoutePathResolver.resolveUriValues(receiver).isNotEmpty()
    }

    companion object {

        private const val PATH_CALL = "path"
        private val NESTING_CALLS = setOf("nest", PATH_CALL)
        private val STRING_TYPES = setOf(CommonClassNames.JAVA_LANG_STRING, "kotlin.String")

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

        private fun nameStartOf(call: UCallExpression): Int =
            (call.methodIdentifier?.sourcePsi ?: call.sourcePsi)?.textRange?.startOffset ?: Int.MAX_VALUE

        private fun fromCall(call: UCallExpression): FunctionalRouteTarget? {
            if (call.methodName !in SpringWebClasses.ROUTER_DSL_ROUTE_METHODS) return null
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
