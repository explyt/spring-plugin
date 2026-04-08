/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.psi

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.uast.UField
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.toUElementOfType

@Service(Service.Level.PROJECT)
class JpqlTypeResolver {
    fun resolveType(expression: JpqlExpression): JpqlType = CachedValuesManager.getCachedValue(expression) {
        CachedValueProvider.Result.create(
            doResolveType(expression),
            PsiModificationTracker.MODIFICATION_COUNT
        )
    }

    private fun doResolveType(expression: JpqlExpression) = when (expression) {
        is JpqlBooleanLiteral,
        is JpqlConditionalAndExpression,
        is JpqlConditionalOrExpression,
        is JpqlConditionalNotExpression,
        is JpqlComparisonExpression,
        is JpqlNullComparisonExpression,
        is JpqlInExpression,
        -> JpqlType.Boolean

        is JpqlStringLiteral,
        is JpqlStringFunctionExpression -> JpqlType.String

        is JpqlDatetimeLiteral,
        is JpqlDatetimeFunctionExpression -> JpqlType.Datetime

        is JpqlTypeLiteral -> JpqlType.Type

        is JpqlNumericLiteral,
        is JpqlFunctionsReturningNumericsExpression,
        is JpqlAggregateExpression -> JpqlType.Numeric

        is JpqlNullExpression -> JpqlType.Null

        is JpqlParenExpression -> expression.expression?.type ?: JpqlType.Unknown

        is JpqlPathReferenceExpression -> resolveReference(expression)

        is JpqlInputParameterExpression -> resolveInputParameter(expression)

        is JpqlSubqueryExpression -> resolveSubquery(expression)


        else -> JpqlType.Unknown
    }

    private fun resolveSubquery(expression: JpqlSubqueryExpression): JpqlType {
        val simpleSelectClause = expression.subquery.simpleSelectClause
        return simpleSelectClause.expression.type
    }

    private fun resolveInputParameter(expression: JpqlInputParameterExpression): JpqlType {
        val psiElement = expression.reference.resolve()
            ?: return JpqlType.Unknown

        val uVariable = psiElement.toUElementOfType<UVariable>()
        if (uVariable != null) {
            return JpqlType.fromPsiType(uVariable.type)
        }

        return JpqlType.Unknown
    }

    private fun resolveReference(expression: JpqlPathReferenceExpression): JpqlType {
        val identifier = expression.identifierList.lastOrNull()
            ?: return JpqlType.Unknown

        return resolveIdentifierType(identifier)
    }

    fun resolveIdentifierType(identifier: JpqlIdentifier): JpqlType {
        val resolve = identifier.reference?.resolve()
            ?: return JpqlType.Unknown

        val uField = resolve.toUElementOfType<UField>()
            ?: return JpqlType.Unknown

        @Suppress("UElementAsPsi")
        return JpqlType.fromPsiType(uField.type)
    }

    companion object {
        fun getInstance(project: Project): JpqlTypeResolver = project.service()
    }
}