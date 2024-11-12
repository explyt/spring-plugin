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
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.PsiClass
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

class SpringWebCoRouterLoader : EndpointHandler {

    override fun handleEndpoints(componentPsiClass: PsiClass): List<EndpointElement> {
        val uClass = componentPsiClass.toUElementOfType<UClass>() ?: return emptyList()

        val routeFunctionMethods = uClass.methods.asSequence()
            .filter { it.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .filter { it.returnType?.isInheritorOf(SpringWebClasses.ROUTE_FUNCTION) == true }
            .toSet()

        return routeFunctionMethods.flatMap { extractEndpoints(it, componentPsiClass) }
    }

    private fun extractEndpoints(uMethod: UMethod, psiClass: PsiClass): List<EndpointElement> {
        val endpoints = mutableListOf<EndpointElement>()

        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.methodName == "coRouter") {
                    val lambdaExpression = node.valueArguments.firstOrNull() as? ULambdaExpression
                    lambdaExpression?.body?.accept(object : AbstractUastVisitor() {
                        override fun visitCallExpression(node: UCallExpression): Boolean {
                            if (node.methodName in SpringWebClasses.URI_TYPE) {
                                val endpointElement = createEndpointElement(node, psiClass)
                                if (endpointElement != null) {
                                    endpoints.add(endpointElement)
                                }
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

    private fun createEndpointElement(callExpression: UCallExpression, psiClass: PsiClass): EndpointElement? {
        val path = SpringWebUtil.getPathFromCallExpression(callExpression)
        val requestMethods = listOf(callExpression.methodName ?: return null)
        val psiElement = callExpression.sourcePsi ?: return null

        return EndpointElement(
            path,
            requestMethods,
            psiElement,
            psiClass,
            null,
            EndpointType.SPRING_WEBFLUX
        )
    }
}
