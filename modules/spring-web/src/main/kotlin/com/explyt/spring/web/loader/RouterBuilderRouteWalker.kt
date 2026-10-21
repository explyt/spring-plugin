/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.route.RoutePredicates
import com.explyt.spring.web.route.RoutePrefix
import com.explyt.spring.web.route.RouteScope
import com.explyt.spring.web.util.RoutePathResolver
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiUtil
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

data class BuilderRoute(val path: String, val verb: String, val call: PsiMethodCallExpression)

object RouterBuilderRouteWalker {

    private const val MAX_NESTING = 8

    fun routesOf(beanMethod: PsiMethod): List<BuilderRoute> = walkOf(beanMethod).routes

    class BeanMethodWalk(val modelled: Boolean, val routes: List<BuilderRoute>)

    fun walkOf(beanMethod: PsiMethod): BeanMethodWalk =
        CachedValuesManager.getCachedValue(beanMethod) {
            val router = returnedRouterOf(beanMethod)
            CachedValueProvider.Result(
                BeanMethodWalk(
                    router != null && callChainOf(router).any { it.isRouteRegistration() },
                    router?.let { walkChain(it, RouteScope.ROOT, 0) }.orEmpty()
                ),
                ModificationTrackerManager.getInstance(beanMethod.project).getUastModelAndLibraryTracker(),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

    private fun PsiMethodCallExpression.isRouteRegistration(): Boolean {
        ProgressManager.checkCanceled()
        val name = methodExpression.referenceName
        if (name !in SpringWebClasses.ROUTE_FUNCTION_BUILDER_VERBS
            && name != SpringWebClasses.ROUTE_PATH
            && name != SpringWebClasses.ROUTE_NEST
        ) return false
        return resolveMethod()?.containingClass?.qualifiedName in SpringWebClasses.ROUTE_FUNCTION_BUILDERS
    }

    private fun returnedRouterOf(beanMethod: PsiMethod): PsiExpression? =
        beanMethod.body?.statements?.firstNotNullOfOrNull { (it as? PsiReturnStatement)?.returnValue }

    private fun walkChain(expression: PsiExpression, scope: RouteScope, nesting: Int): List<BuilderRoute> {
        if (nesting > MAX_NESTING) return emptyList()
        return callChainOf(expression).toList().asReversed().flatMap { routesOfCall(it, scope, nesting) }
    }

    private fun callChainOf(expression: PsiExpression?): Sequence<PsiMethodCallExpression> =
        generateSequence(unwrap(expression) as? PsiMethodCallExpression) {
            unwrap(it.methodExpression.qualifierExpression) as? PsiMethodCallExpression
        }

    private fun routesOfCall(call: PsiMethodCallExpression, scope: RouteScope, nesting: Int): List<BuilderRoute> {
        ProgressManager.checkCanceled()
        val method = call.resolveMethod()
            ?.takeIf { it.containingClass?.qualifiedName in SpringWebClasses.ROUTE_FUNCTION_BUILDERS }
            ?: return emptyList()
        val arguments = call.argumentList.expressions
        return when {
            method.name in SpringWebClasses.ROUTE_FUNCTION_BUILDER_VERBS ->
                verbRoutes(call, method, arguments.firstOrNull(), scope)

            arguments.size != 2 -> emptyList()

            method.name == SpringWebClasses.ROUTE_PATH ->
                nestedRoutes(RouteScope.of(RoutePrefix.of(uriValues(arguments[0]))), arguments[1], scope, nesting)

            method.name == SpringWebClasses.ROUTE_NEST ->
                nestedRoutes(predicateScope(arguments[0]), arguments[1], scope, nesting)

            else -> emptyList()
        }
    }

    private fun verbRoutes(
        call: PsiMethodCallExpression,
        method: PsiMethod,
        uriArgument: PsiExpression?,
        scope: RouteScope
    ): List<BuilderRoute> {
        if (!method.takesUriFirst()) return emptyList()
        val verb = scope.verbs.servedVerbsOf(listOf(method.name))?.singleOrNull() ?: return emptyList()
        val uris = uriValues(uriArgument)
        val paths = when (val prefix = scope.prefix) {
            RoutePrefix.PathFree -> uris.filter { it.isNotEmpty() }
            is RoutePrefix.Decided -> (prefix.then(RoutePrefix.of(uris)) as? RoutePrefix.Decided)
                ?.segments?.map(SpringWebUtil::simplifyUrl).orEmpty()

            RoutePrefix.Undecidable -> emptyList()
        }
        return paths.map { BuilderRoute(it, verb, call) }
    }

    private fun nestedRoutes(
        nestScope: RouteScope,
        callback: PsiExpression,
        scope: RouteScope,
        nesting: Int
    ): List<BuilderRoute> {
        val innerScope = scope.then(nestScope)
        if (innerScope.isUndecidable) return emptyList()
        return callbackRoutes(callback, innerScope, nesting + 1)
    }

    private fun callbackRoutes(callback: PsiExpression, scope: RouteScope, nesting: Int): List<BuilderRoute> {
        val lambda = unwrap(callback) as? PsiLambdaExpression ?: return emptyList()
        val builderParameter = lambda.parameterList.parameters.singleOrNull()
        val chains = if (builderParameter == null) {
            returnedExpressions(lambda.body)
        } else {
            statementExpressions(lambda.body).filter { it.isChainRootedAt(builderParameter) }
        }
        return chains.flatMap { walkChain(it, scope, nesting) }
    }

    private fun predicateScope(predicate: PsiExpression): RouteScope =
        predicate.toUElementOfType<UExpression>()?.let(RoutePredicates::scopeOf) ?: RouteScope.UNDECIDABLE

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

    private fun unwrap(expression: PsiExpression?): PsiExpression? = PsiUtil.skipParenthesizedExprDown(expression)
}
