package com.esprito.spring.web.service.beans.discoverer

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebClasses
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
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
                            if (node.methodName in listOf("GET", "POST", "PUT", "PATCH", "DELETE")) {
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
        val path = getPathFromCall(callExpression)
        val requestMethods = listOf(callExpression.methodName ?: return null)
        val psiElement = callExpression.sourcePsi ?: return null

        return EndpointElement(path, requestMethods, psiElement, psiClass)
    }

    private fun getPathFromCall(callExpression: UCallExpression): String {
        var path = ""

        var currentNode = callExpression as? UElement
        while (currentNode != null) {
            if (currentNode is UCallExpression) {
                if (currentNode.methodName == "nest") {
                    val currentNodeParent = currentNode.uastParent
                    if (currentNodeParent is UExpression) {
                        val qualifiedExpression = currentNodeParent.sourcePsi
                        if (qualifiedExpression is KtDotQualifiedExpression) {
                            path = getUri(qualifiedExpression, path)
                        }
                    }
                } else {
                    val argument = currentNode.valueArguments.firstOrNull()
                    if (argument is UPolyadicExpression) {
                        val operand = argument.operands.firstOrNull()
                        if (operand is ULiteralExpression) {
                            path = "$path${operand.value}"
                        }
                    }
                }
            }
            currentNode = currentNode.uastParent
        }

        return path
    }

    private fun getUri(statement: KtDotQualifiedExpression, path: String): String {
        val receiver = statement.receiverExpression
        if (receiver is KtStringTemplateExpression) {
            val uri = receiver.entries.joinToString("") { it.text }
            return "$uri$path"
        }
        return path
    }
}
