/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

class SpringWebCoRouterLoader : EndpointHandler {

    override fun handleEndpoints(componentPsiClass: PsiClass): List<EndpointElement> {
        val uClass = componentPsiClass.toUElementOfType<UClass>() ?: return emptyList()

        return uClass.methods.asSequence()
            .filter { it.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .mapNotNull { method -> endpointTypeOf(method.returnType)?.let { method to it } }
            .toSet()
            .flatMap { (method, endpointType) -> extractEndpoints(method, componentPsiClass, endpointType) }
    }

    /**
     * The same DSL shape serves both stacks, so the bean's own type decides which one a route belongs to — the DSL
     * entry point is named `router` in each of them.
     */
    private fun endpointTypeOf(returnType: PsiType?): EndpointType? = when {
        returnType == null -> null
        returnType.isInheritorOf(SpringWebClasses.ROUTE_FUNCTION) -> EndpointType.SPRING_WEBFLUX
        returnType.isInheritorOf(SpringWebClasses.SERVLET_ROUTE_FUNCTION) -> EndpointType.SPRING_MVC
        else -> null
    }

    private fun extractEndpoints(
        uMethod: UMethod,
        psiClass: PsiClass,
        endpointType: EndpointType
    ): List<EndpointElement> {
        val endpoints = mutableListOf<EndpointElement>()

        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.methodName in SpringWebClasses.ROUTER_DSL_ENTRY_POINTS) {
                    val lambdaExpression = node.valueArguments.firstOrNull() as? ULambdaExpression
                    lambdaExpression?.body?.accept(object : AbstractUastVisitor() {
                        override fun visitCallExpression(node: UCallExpression): Boolean {
                            if (node.methodName in SpringWebClasses.ROUTER_DSL_ROUTE_METHODS) {
                                endpoints += createEndpointElements(node, psiClass, endpointType)
                            }
                            return super.visitCallExpression(node)
                        }
                    })
                }
                return super.visitCallExpression(node)
            }
        })

        return endpoints
    }

    /**
     * A route whose URI does not resolve to a value yields no endpoint: an empty path is normalised to `/` downstream
     * and would register the route as the application root.
     */
    private fun createEndpointElements(
        callExpression: UCallExpression,
        psiClass: PsiClass,
        endpointType: EndpointType
    ): List<EndpointElement> {
        val requestMethods = listOf(SpringWebUtil.getRequestMethod(callExpression) ?: return emptyList())
        val psiElement = callExpression.sourcePsi ?: return emptyList()

        return SpringWebUtil.getPathsFromCallExpression(callExpression).asSequence()
            .filter { it.isNotEmpty() }
            .map {
                EndpointElement(
                    it,
                    requestMethods,
                    psiElement,
                    psiClass,
                    null,
                    endpointType
                )
            }
            .toList()
    }
}
