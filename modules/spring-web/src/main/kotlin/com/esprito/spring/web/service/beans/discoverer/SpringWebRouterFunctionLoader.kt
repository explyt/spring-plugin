package com.esprito.spring.web.service.beans.discoverer

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.web.SpringWebClasses
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiLiteralUtil
import com.intellij.psi.util.childrenOfType

class SpringWebRouterFunctionLoader(private val project: Project) : SpringWebEndpointsLoader {
    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val componentAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringCoreClasses.COMPONENT, false
        )

        return componentAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, module.moduleWithDependenciesScope) }
            .flatMap { getEndpoints(it) }
            .toList()
    }

    private fun getEndpoints(
        componentPsiClass: PsiClass
    ): List<EndpointElement> {
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
                    result += EndpointElement(url, listOf(methods.name), psiMethod, containingClass)
                    result += findSimpleRouteMethod(methodCallException, psiMethod, containingClass)
                }
            }
        }
        return result
    }
}