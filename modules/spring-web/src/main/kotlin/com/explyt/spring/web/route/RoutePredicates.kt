/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.route

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.RoutePathResolver
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

object RoutePredicates {

    private const val MAX_DEPTH = 16

    fun prefixOf(expression: UExpression): RoutePrefix = prefixOf(expression, 0)

    fun prefixOfNest(nestCall: UCallExpression): RoutePrefix =
        nestPredicate(nestCall)?.let(::prefixOf) ?: RoutePrefix.Undecidable

    fun returnsRequestPredicate(call: UCallExpression): Boolean = call.resolve()?.returnsRequestPredicate() == true

    private fun nestPredicate(nestCall: UCallExpression): UExpression? =
        nestCall.receiver ?: nestCall.valueArguments.takeIf { it.size == 2 }?.first()

    private fun prefixOf(expression: UExpression, depth: Int): RoutePrefix {
        ProgressManager.checkCanceled()
        if (depth > MAX_DEPTH) return RoutePrefix.Undecidable

        val values = RoutePathResolver.resolveUriValues(expression)
        if (values.isNotEmpty()) return RoutePrefix.Decided(values)

        return when (expression) {
            is UParenthesizedExpression -> prefixOf(expression.expression, depth + 1)
            is UQualifiedReferenceExpression ->
                (expression.selector as? UCallExpression)?.let { prefixOfCall(it, depth + 1) }
                    ?: RoutePrefix.Undecidable

            is UCallExpression -> prefixOfCall(expression, depth + 1)
            is UBinaryExpression -> prefixOfOperator(expression, depth + 1)
            else -> RoutePrefix.Undecidable
        }
    }

    private fun prefixOfCall(call: UCallExpression, depth: Int): RoutePrefix {
        val method = call.resolve() ?: return RoutePrefix.Undecidable
        val argument = call.valueArguments.singleOrNull()
        return when {
            method.isPredicateAnd() && argument != null -> {
                val receiver = call.receiver ?: return RoutePrefix.Undecidable
                prefixOf(receiver, depth).then(prefixOf(argument, depth))
            }

            !method.isDeclaredIn(SpringWebClasses.REQUEST_PREDICATE_FACTORY_CLASSES) -> RoutePrefix.Undecidable

            method.isPathPredicateFactory() && argument != null ->
                RoutePrefix.of(RoutePathResolver.resolveUriValues(argument))

            method.name in SpringWebClasses.REQUEST_PREDICATES_PATH_FREE_FACTORIES -> RoutePrefix.PathFree
            else -> RoutePrefix.Undecidable
        }
    }

    private fun prefixOfOperator(operation: UBinaryExpression, depth: Int): RoutePrefix {
        if (operation.resolveOperator()?.isPredicateAnd() != true) return RoutePrefix.Undecidable
        return prefixOf(operation.leftOperand, depth).then(prefixOf(operation.rightOperand, depth))
    }

    private fun PsiMethod.isPredicateAnd(): Boolean =
        name == SpringWebClasses.REQUEST_PREDICATE_AND &&
                isDeclaredIn(SpringWebClasses.REQUEST_PREDICATE_OPERATOR_CLASSES) &&
                returnsRequestPredicate()

    private fun PsiMethod.isPathPredicateFactory(): Boolean =
        (name == SpringWebClasses.ROUTE_PATH || name in SpringWebClasses.ROUTE_FUNCTION_BUILDER_VERBS) &&
                returnsRequestPredicate()

    private fun PsiMethod.returnsRequestPredicate(): Boolean =
        returnType?.canonicalText in SpringWebClasses.REQUEST_PREDICATE_CLASSES

    private fun PsiMethod.isDeclaredIn(classNames: Collection<String>): Boolean =
        containingClass?.qualifiedName in classNames
}
