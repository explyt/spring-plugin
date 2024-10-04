package com.esprito.spring.web.service.beans.discoverer

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
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

        return EndpointElement(path, requestMethods, psiElement, psiClass, EndpointType.SPRING_WEBFLUX)
    }
}
