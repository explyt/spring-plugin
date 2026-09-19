/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.RoutePathResolver
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.toUElementOfType

class SpringWebRouterFunctionLoader : EndpointHandler {

    override fun handleEndpoints(componentPsiClass: PsiClass): List<EndpointElement> {
        return componentPsiClass.methods.asSequence()
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .mapNotNull { method -> endpointTypeOf(method.returnType)?.let { method to it } }
            .toSet()
            .flatMap { (method, endpointType) -> getRouteFunctionUrl(componentPsiClass, method, endpointType) }
    }

    /**
     * The builder API is mirrored in both stacks, so the bean's own type decides which one a route belongs to.
     */
    private fun endpointTypeOf(returnType: PsiType?): EndpointType? = when {
        returnType == null -> null
        returnType.isInheritorOf(SpringWebClasses.ROUTE_FUNCTION) -> EndpointType.SPRING_WEBFLUX
        returnType.isInheritorOf(SpringWebClasses.SERVLET_ROUTE_FUNCTION) -> EndpointType.SPRING_MVC
        else -> null
    }

    private fun getRouteFunctionUrl(
        containingClass: PsiClass,
        psiMethod: PsiMethod,
        endpointType: EndpointType
    ): List<EndpointElement> {
        val codeBlock = psiMethod.childrenOfType<PsiCodeBlock>().firstOrNull() ?: return emptyList()
        val returnStatement = codeBlock.childrenOfType<PsiReturnStatement>().firstOrNull() ?: return emptyList()
        val returnValue = returnStatement.returnValue ?: return emptyList()
        return findSimpleRouteMethod(returnValue, psiMethod, containingClass, endpointType)
    }

    private fun findSimpleRouteMethod(
        expression: PsiExpression,
        psiMethod: PsiMethod,
        containingClass: PsiClass,
        endpointType: EndpointType
    ): List<EndpointElement> {
        val result = mutableListOf<EndpointElement>()

        val refException = expression.childrenOfType<PsiReferenceExpression>().firstOrNull() ?: return emptyList()
        val methodCallException =
            refException.childrenOfType<PsiMethodCallExpression>().firstOrNull() ?: return emptyList()
        val methods = methodCallException.resolveMethod() ?: return emptyList()

        if (methods.containingClass?.qualifiedName in SpringWebClasses.ROUTE_FUNCTION_BUILDERS) {
            val uriArgument = methodCallException.argumentList.expressions.firstOrNull()
                ?.toUElementOfType<UExpression>()
            val urls = uriArgument
                ?.let { RoutePathResolver.resolveUriValues(it) }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()

            if (urls.isNotEmpty()) {
                urls.mapTo(result) {
                    EndpointElement(
                        it,
                        listOf(methods.name),
                        psiMethod,
                        containingClass,
                        null,
                        endpointType
                    )
                }
                result += findSimpleRouteMethod(methodCallException, psiMethod, containingClass, endpointType)
            }
        }
        return result
    }
}
