/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.RoutePathResolver
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

data class BuilderRoute(val path: String, val verb: String)

object RouterBuilderRouteWalker {

    private const val MAX_NESTING = 8

    fun routesOf(builderExpression: PsiExpression): List<BuilderRoute> = walkChain(builderExpression, "", 0)

    private fun walkChain(expression: PsiExpression, prefix: String, nesting: Int): List<BuilderRoute> {
        if (nesting > MAX_NESTING) return emptyList()
        return callChainOf(expression).toList().asReversed().flatMap { routesOfCall(it, prefix, nesting) }
    }

    private fun callChainOf(expression: PsiExpression?): Sequence<PsiMethodCallExpression> =
        generateSequence(unwrap(expression) as? PsiMethodCallExpression) {
            unwrap(it.methodExpression.qualifierExpression) as? PsiMethodCallExpression
        }

    private fun routesOfCall(call: PsiMethodCallExpression, prefix: String, nesting: Int): List<BuilderRoute> {
        ProgressManager.checkCanceled()
        val method = call.resolveMethod()
            ?.takeIf { it.containingClass?.qualifiedName in SpringWebClasses.ROUTE_FUNCTION_BUILDERS }
            ?: return emptyList()
        val arguments = call.argumentList.expressions
        return when {
            method.name in SpringWebClasses.ROUTE_FUNCTION_BUILDER_VERBS ->
                verbRoutes(method, arguments.firstOrNull(), prefix)

            arguments.size != 2 -> emptyList()

            method.name == SpringWebClasses.ROUTE_FUNCTION_BUILDER_PATH ->
                nestedRoutes(uriValues(arguments[0]), arguments[1], prefix, nesting)

            method.name == SpringWebClasses.ROUTE_FUNCTION_BUILDER_NEST ->
                nestedRoutes(predicatePrefixes(arguments[0]), arguments[1], prefix, nesting)

            else -> emptyList()
        }
    }

    private fun verbRoutes(method: PsiMethod, uriArgument: PsiExpression?, prefix: String): List<BuilderRoute> {
        if (!method.takesUriFirst()) return emptyList()
        return uriValues(uriArgument).mapNotNull { uri ->
            when {
                prefix.isNotEmpty() -> SpringWebUtil.simplifyUrl(join(prefix, uri))
                uri.isNotEmpty() -> uri
                else -> null
            }?.let { BuilderRoute(it, method.name) }
        }
    }

    private fun nestedRoutes(
        segments: List<String>,
        callback: PsiExpression,
        prefix: String,
        nesting: Int
    ): List<BuilderRoute> = segments.flatMap { callbackRoutes(callback, join(prefix, it), nesting + 1) }

    private fun callbackRoutes(callback: PsiExpression, prefix: String, nesting: Int): List<BuilderRoute> {
        val lambda = unwrap(callback) as? PsiLambdaExpression ?: return emptyList()
        val builderParameter = lambda.parameterList.parameters.singleOrNull()
        val chains = if (builderParameter == null) {
            returnedExpressions(lambda.body)
        } else {
            statementExpressions(lambda.body).filter { it.isChainRootedAt(builderParameter) }
        }
        return chains.flatMap { walkChain(it, prefix, nesting) }
    }

    private fun predicatePrefixes(predicate: PsiExpression): List<String> {
        val call = unwrap(predicate) as? PsiMethodCallExpression ?: return listOf("")
        val method = call.resolveMethod()
        val isPathPredicate = method != null &&
                method.name == SpringWebClasses.REQUEST_PREDICATES_PATH &&
                method.parameterList.parametersCount == 1 &&
                method.containingClass?.qualifiedName in SpringWebClasses.REQUEST_PREDICATES_CLASSES
        return if (isPathPredicate) uriValues(call.argumentList.expressions.singleOrNull()) else listOf("")
    }

    private fun returnedExpressions(body: PsiElement?): List<PsiExpression> = when (body) {
        is PsiExpression -> listOf(body)
        is PsiCodeBlock -> body.statements.filterIsInstance<PsiReturnStatement>().mapNotNull { it.returnValue }
        else -> emptyList()
    }

    private fun statementExpressions(body: PsiElement?): List<PsiExpression> = when (body) {
        is PsiExpression -> listOf(body)
        is PsiCodeBlock -> body.statements.filterIsInstance<PsiExpressionStatement>().map { it.expression }
        else -> emptyList()
    }

    private fun PsiExpression.isChainRootedAt(parameter: PsiParameter): Boolean {
        val root = callChainOf(this).lastOrNull()?.methodExpression?.qualifierExpression
        return (unwrap(root) as? PsiReferenceExpression)?.resolve() == parameter
    }

    private fun PsiMethod.takesUriFirst(): Boolean =
        parameterList.parameters.firstOrNull()?.type?.equalsToText(CommonClassNames.JAVA_LANG_STRING) == true

    private fun uriValues(expression: PsiExpression?): List<String> =
        expression?.toUElementOfType<UExpression>()?.let(RoutePathResolver::resolveUriValues).orEmpty()

    private fun join(prefix: String, segment: String): String = when {
        prefix.isEmpty() -> segment
        segment.isEmpty() -> prefix
        else -> "$prefix/$segment"
    }

    private fun unwrap(expression: PsiExpression?): PsiExpression? = PsiUtil.skipParenthesizedExprDown(expression)
}
