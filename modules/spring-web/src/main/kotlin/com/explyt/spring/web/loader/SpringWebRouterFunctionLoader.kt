/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.*
import com.intellij.psi.util.PsiLiteralUtil
import com.intellij.psi.util.childrenOfType

class SpringWebRouterFunctionLoader : EndpointHandler {

    override fun handleEndpoints(componentPsiClass: PsiClass): List<EndpointElement> {
        val routeFunctionMethods = componentPsiClass.methods
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .filter { it.returnType?.isInheritorOf(SpringWebClasses.ROUTE_FUNCTION) == true }
            .toSet()

        return routeFunctionMethods.flatMap { getRouteFunctionUrl(componentPsiClass, it) }
    }

    private fun getRouteFunctionUrl(containingClass: PsiClass, psiMethod: PsiMethod): List<EndpointElement> {
        val codeBlock = psiMethod.childrenOfType<PsiCodeBlock>().firstOrNull() ?: return emptyList()
        val returnStatement = codeBlock.childrenOfType<PsiReturnStatement>().firstOrNull() ?: return emptyList()
        val returnValue = returnStatement.returnValue ?: return emptyList()
        return findSimpleRouteMethod(returnValue, psiMethod, containingClass)
    }

    private fun findSimpleRouteMethod(
        expression: PsiExpression,
        psiMethod: PsiMethod,
        containingClass: PsiClass
    ): List<EndpointElement> {
        val result = mutableListOf<EndpointElement>()

        val refException = expression.childrenOfType<PsiReferenceExpression>().firstOrNull() ?: return emptyList()
        val methodCallException =
            refException.childrenOfType<PsiMethodCallExpression>().firstOrNull() ?: return emptyList()
        val methods = methodCallException.resolveMethod() ?: return emptyList()

        if (methods.containingClass?.qualifiedName == SpringWebClasses.ROUTE_FUNCTION_BUILDER) {
            val psiLiteralExpression = methodCallException.argumentList.expressions.firstOrNull()
            if (psiLiteralExpression != null && psiLiteralExpression is PsiLiteralExpression) {
                val url = PsiLiteralUtil.getStringLiteralContent(psiLiteralExpression)
                if (url != null) {
                    result += EndpointElement(
                        url,
                        listOf(methods.name),
                        psiMethod,
                        containingClass,
                        null,
                        EndpointType.SPRING_WEBFLUX
                    )
                    result += findSimpleRouteMethod(methodCallException, psiMethod, containingClass)
                }
            }
        }
        return result
    }
}