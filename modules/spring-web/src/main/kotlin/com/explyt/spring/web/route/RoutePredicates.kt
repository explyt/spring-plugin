/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.route

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.RoutePathResolver
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.uast.*

object RoutePredicates {

    private const val MAX_DEPTH = 16

    fun scopeOf(expression: UExpression): RouteScope = scopeOf(expression, 0)

    fun scopeOfNest(nestCall: UCallExpression): RouteScope =
        nestPredicate(nestCall)?.let(::scopeOf) ?: RouteScope.UNDECIDABLE

    fun prefixOfNest(nestCall: UCallExpression): RoutePrefix = scopeOfNest(nestCall).prefix

    fun returnsRequestPredicate(call: UCallExpression): Boolean = call.resolve()?.returnsRequestPredicate() == true

    private fun nestPredicate(nestCall: UCallExpression): UExpression? =
        nestCall.receiver ?: nestCall.valueArguments.takeIf { it.size == 2 }?.first()

    private fun scopeOf(expression: UExpression, depth: Int): RouteScope {
        ProgressManager.checkCanceled()
        if (depth > MAX_DEPTH) return RouteScope.UNDECIDABLE

        val values = RoutePathResolver.resolveUriValues(expression)
        if (values.isNotEmpty()) return RouteScope.of(RoutePrefix.Decided(values))

        return when (expression) {
            is UParenthesizedExpression -> scopeOf(expression.expression, depth + 1)
            is UQualifiedReferenceExpression ->
                (expression.selector as? UCallExpression)?.let { scopeOfCall(it, depth + 1) }
                    ?: RouteScope.UNDECIDABLE

            is UCallExpression -> scopeOfCall(expression, depth + 1)
            is UBinaryExpression -> scopeOfOperator(expression, depth + 1)
            else -> RouteScope.UNDECIDABLE
        }
    }

    private fun scopeOfCall(call: UCallExpression, depth: Int): RouteScope {
        val method = call.resolve() ?: return RouteScope.UNDECIDABLE
        val argument = call.valueArguments.singleOrNull()
        return when {
            method.isPredicateAnd() && argument != null -> {
                val receiver = call.receiver ?: return RouteScope.UNDECIDABLE
                scopeOf(receiver, depth).then(scopeOf(argument, depth))
            }

            !method.isDeclaredIn(SpringWebClasses.REQUEST_PREDICATE_FACTORY_CLASSES) -> RouteScope.UNDECIDABLE

            method.isPathPredicateFactory() && argument != null ->
                RouteScope.of(RoutePrefix.of(RoutePathResolver.resolveUriValues(argument)))

            method.isVerbPredicateFactory() && argument != null -> RouteScope(
                RoutePrefix.of(RoutePathResolver.resolveUriValues(argument)),
                RouteVerbs.Only(setOf(method.name))
            )

            method.name in SpringWebClasses.REQUEST_PREDICATES_METHOD_FACTORIES ->
                RouteScope(RoutePrefix.PathFree, RouteVerbs.of(call.valueArguments.map { it.httpMethodName() }))

            method.name in SpringWebClasses.REQUEST_PREDICATES_PATH_FREE_FACTORIES -> RouteScope.ROOT
            else -> RouteScope.UNDECIDABLE
        }
    }

    private fun scopeOfOperator(operation: UBinaryExpression, depth: Int): RouteScope {
        if (operation.resolveOperator()?.isPredicateAnd() != true) return RouteScope.UNDECIDABLE
        return scopeOf(operation.leftOperand, depth).then(scopeOf(operation.rightOperand, depth))
    }

    private fun UExpression.httpMethodName(): String? {
        val field = (skipParenthesizedExprDown() as? UResolvable)?.resolve() as? PsiField ?: return null
        return field.name.takeIf {
            field.hasModifierProperty(PsiModifier.STATIC) &&
                    field.containingClass?.qualifiedName == SpringWebClasses.HTTP_METHOD
        }
    }

    private fun PsiMethod.isPredicateAnd(): Boolean =
        name == SpringWebClasses.REQUEST_PREDICATE_AND &&
                isDeclaredIn(SpringWebClasses.REQUEST_PREDICATE_OPERATOR_CLASSES) &&
                returnsRequestPredicate()

    private fun PsiMethod.isPathPredicateFactory(): Boolean =
        name == SpringWebClasses.ROUTE_PATH && returnsRequestPredicate()

    private fun PsiMethod.isVerbPredicateFactory(): Boolean =
        name in SpringWebClasses.ROUTE_FUNCTION_BUILDER_VERBS && returnsRequestPredicate()

    private fun PsiMethod.returnsRequestPredicate(): Boolean =
        returnType?.canonicalText in SpringWebClasses.REQUEST_PREDICATE_CLASSES

    private fun PsiMethod.isDeclaredIn(classNames: Collection<String>): Boolean =
        containingClass?.qualifiedName in classNames
}
