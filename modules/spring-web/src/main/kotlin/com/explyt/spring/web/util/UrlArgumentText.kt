/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPolyadicExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.expressions.UInjectionHost

/**
 * The URL a test request is sent to, read from the expression that builds it.
 *
 * A test rarely writes the URL as one constant. It interpolates an id - `"/api/items/$id"` in Kotlin,
 * `"/api/items/" + id` in Java - or wraps the text in `URI.create(...)` or `new URI(...)`. Evaluating such an
 * expression yields nothing, so the request looked like it addressed no endpoint. Every part that is not a compile-time
 * constant is read as one path segment, [VARIABLE_SEGMENT], which a route template matches like any `{name}`.
 */
object UrlArgumentText {

    /** A part of the URL whose value is known only at run time; a route's `{name}` segment matches it. */
    const val VARIABLE_SEGMENT = "{*}"

    /**
     * The URL text of [expression], or `null` when it is neither a string nor a `java.net.URI` built from one, or when
     * nothing of it is known before run time - `get(url)` says nothing about which endpoint it calls, and read as one
     * variable segment it would match every route with a template at its root.
     */
    fun of(expression: UExpression): String? {
        val unwrapped = unwrap(expression)
        uriArgumentOf(unwrapped)?.let { return of(it) }
        if (!isString(unwrapped.getExpressionType())) return null
        return textOf(unwrapped).takeIf { it.replace(VARIABLE_SEGMENT, "").trim('/').isNotBlank() }
    }

    /** Whether [method] takes the request URL as a `java.net.URI` argument, and at which index. */
    fun uriParameterIndex(method: PsiMethod): Int =
        method.parameterList.parameters.indexOfFirst { it.type.canonicalText == JAVA_NET_URI }

    private fun textOf(expression: UExpression): String {
        expression.evaluateString()?.let { return it }
        return when (expression) {
            is UInjectionHost -> expression.evaluateToString() ?: interpolated(expression)
            is UPolyadicExpression ->
                if (isConcatenation(expression)) expression.operands.joinToString("") { textOf(unwrap(it)) }
                else VARIABLE_SEGMENT
            is ULiteralExpression -> expression.value?.toString() ?: VARIABLE_SEGMENT
            else -> VARIABLE_SEGMENT
        }
    }

    /** A Kotlin string template: its literal entries as written, every `$x` and `${...}` as a variable segment. */
    private fun interpolated(host: UInjectionHost): String =
        if (host is UPolyadicExpression) host.operands.joinToString("") { textOf(unwrap(it)) } else VARIABLE_SEGMENT

    private fun uriArgumentOf(expression: UExpression): UExpression? {
        val call = (expression as? UQualifiedReferenceExpression)?.selector as? UCallExpression
            ?: expression as? UCallExpression
            ?: return null
        val method = call.resolve() ?: return null
        if (method.containingClass?.qualifiedName != JAVA_NET_URI) return null
        if (!method.isConstructor && method.name != URI_FACTORY) return null
        return call.valueArguments.singleOrNull()
    }

    /** A `+` that joins text, not `page + 1` inside a template entry, which is one run-time value. */
    private fun isConcatenation(expression: UPolyadicExpression): Boolean =
        expression.operator == UastBinaryOperator.PLUS && isString(expression.getExpressionType())

    private fun unwrap(expression: UExpression): UExpression =
        if (expression is UParenthesizedExpression) unwrap(expression.expression) else expression

    private fun isString(type: PsiType?): Boolean =
        type == null || type.canonicalText == CommonClassNames.JAVA_LANG_STRING || type.canonicalText == KOTLIN_STRING

    private const val JAVA_NET_URI = "java.net.URI"
    private const val URI_FACTORY = "create"
    private const val KOTLIN_STRING = "kotlin.String"
}
