package com.esprito.spring.web.loader

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.web.SpringWebClasses
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
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