/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.*

object RoutePathResolver {

    private const val MAX_DEPTH = 5
    private const val MAX_VALUES = 100

    private val COLLECTION_FACTORIES =
        setOf("listOf", "listOfNotNull", "setOf", "arrayOf", "mutableListOf", "mutableSetOf")

    private val ITERATING_CALLS = setOf("forEach", "onEach", "forEachIndexed", "map")

    private const val IMPLICIT_PARAMETER = "it"

    fun resolveUriValues(expression: UExpression): List<String> = resolve(expression, 0)

    private fun resolve(expression: UExpression, depth: Int): List<String> {
        ProgressManager.checkCanceled()
        if (depth > MAX_DEPTH) return emptyList()

        expression.evaluateString()?.let { return listOf(it) }

        return when (expression) {
            is UCallExpression -> resolveCollectionFactory(expression, depth)
            is UQualifiedReferenceExpression -> resolve(expression.selector, depth + 1)
            is UReferenceExpression -> resolveReference(expression, depth)
            else -> emptyList()
        }
    }

    private fun resolveCollectionFactory(call: UCallExpression, depth: Int): List<String> {
        if (call.callName() !in COLLECTION_FACTORIES) return emptyList()

        return call.valueArguments.asSequence()
            .flatMap { resolve(it, depth + 1) }
            .take(MAX_VALUES)
            .toList()
    }

    private fun resolveReference(reference: UReferenceExpression, depth: Int): List<String> {
        reference.resolve()
            ?.let { initializerOf(it) }
            ?.let { return resolve(it, depth + 1) }

        return resolveIteratedElement(reference, depth)
    }

    /**
     * A Kotlin `val` resolves to its light getter rather than to a [UField], so the initializer is reached through the
     * navigation element. A Java `static final` field is read as a [UField].
     */
    private fun initializerOf(resolved: PsiElement): UExpression? {
        resolved.toUElementOfType<UField>()?.uastInitializer?.let { return it }

        val property = resolved as? KtProperty ?: resolved.navigationElement as? KtProperty ?: return null
        return property.initializer?.toUElementOfType<UExpression>()
    }

    /**
     * Resolves `path` in `PATHS.forEach { GET(path, handler::handle) }` to the elements of `PATHS`.
     */
    private fun resolveIteratedElement(reference: UReferenceExpression, depth: Int): List<String> {
        val parameterName = (reference as? USimpleNameReferenceExpression)?.identifier ?: return emptyList()

        var current: UElement? = reference.uastParent
        while (current != null) {
            ProgressManager.checkCanceled()
            if (current is ULambdaExpression && current.declaresParameter(parameterName)) {
                val iterated = current.iteratingCall()?.receiver ?: return emptyList()
                return resolve(iterated, depth + 1)
            }
            current = current.uastParent
        }
        return emptyList()
    }

    /**
     * The implicit `it` of a Kotlin lambda is not always reported among the lambda parameters, so a reference named
     * `it` belongs to the nearest lambda that declares no parameter of its own.
     */
    private fun ULambdaExpression.declaresParameter(name: String): Boolean {
        val declaredNames = parameters.mapNotNull { (it.javaPsi as? PsiParameter)?.name }
        return name in declaredNames || (name == IMPLICIT_PARAMETER && declaredNames.none { it != IMPLICIT_PARAMETER })
    }

    private fun ULambdaExpression.iteratingCall(): UCallExpression? =
        (uastParent as? UCallExpression)?.takeIf { it.callName() in ITERATING_CALLS }

    /**
     * A call into an unresolved declaration — a stdlib function absent from the module classpath, for instance —
     * reports no [UCallExpression.methodName], so the name is read from the callee as written.
     */
    private fun UCallExpression.callName(): String? =
        methodName ?: methodIdentifier?.name ?: (sourcePsi as? KtCallExpression)?.calleeExpression?.text
}
