/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.*
import com.intellij.psi.util.childrenOfType

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
        return RouterBuilderRouteWalker.routesOf(returnValue).map { route ->
            EndpointElement(route.path, listOf(route.verb), psiMethod, containingClass, null, endpointType)
        }
    }
}
