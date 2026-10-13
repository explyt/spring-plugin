/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.route

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression

object RouterDslRoutes {

    private const val ANY_METHOD_ROUTE_ARGUMENTS = 2
    private const val STRING_ROUTE_NAME = "invoke"

    fun isRouteName(name: String?): Boolean =
        name in SpringWebClasses.ROUTER_DSL_ROUTE_METHODS || name == SpringWebClasses.ROUTE_PATH

    fun requestMethodsOf(call: UCallExpression): List<String>? = when (call.methodName) {
        in SpringWebClasses.ROUTER_DSL_ROUTE_METHODS -> SpringWebUtil.getRequestMethod(call)?.let { listOf(it) }
        SpringWebClasses.ROUTE_PATH -> emptyList<String>().takeIf { isAnyMethodRoute(call) }
        STRING_ROUTE_NAME -> emptyList<String>().takeIf { isStringRoute(call) }
        else -> null
    }

    fun uriExpressionOf(call: UCallExpression): UExpression? =
        if (isStringRoute(call)) call.receiver else call.valueArguments.firstOrNull()

    private fun isStringRoute(call: UCallExpression): Boolean =
        call.methodName == STRING_ROUTE_NAME &&
                call.receiverType?.equalsToText(CommonClassNames.JAVA_LANG_STRING) == true &&
                call.resolve()?.isStringRouteDeclaration() == true

    private fun PsiMethod.isStringRouteDeclaration(): Boolean =
        name == STRING_ROUTE_NAME &&
                containingClass?.qualifiedName in SpringWebClasses.ROUTER_DSL_CLASSES &&
                returnType == PsiTypes.voidType()

    private fun isAnyMethodRoute(call: UCallExpression): Boolean =
        call.valueArgumentCount == ANY_METHOD_ROUTE_ARGUMENTS && call.resolve()?.isAnyMethodRouteDeclaration() == true

    private fun PsiMethod.isAnyMethodRouteDeclaration(): Boolean {
        val parameters = parameterList.parameters
        return name == SpringWebClasses.ROUTE_PATH &&
                containingClass?.qualifiedName in SpringWebClasses.ROUTER_DSL_CLASSES &&
                parameters.size == ANY_METHOD_ROUTE_ARGUMENTS &&
                parameters.first().type.equalsToText(CommonClassNames.JAVA_LANG_STRING) &&
                returnType == PsiTypes.voidType()
    }
}
