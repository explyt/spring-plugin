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

package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.providers.EndpointUsageSearcher.findMockMvcEndpointUsage
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findWebTestClientEndpointUsage
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

open class RouteFunctionEndpointLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)

        val parameter = if (element.language == KotlinLanguage.INSTANCE) getCoRouteFunction(uParent)
        else getRouteFunction(element)

        if (parameter == null) return
        val (path, methodNames) = parameter

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        result += NavigationGutterIconBuilder.create(SpringIcons.ReadAccess)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy {
                StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_TARGET_ENDPOINTS_ROUTER_FUNCTION)
                findOpenApiJsonEndpoints(path, listOf(methodNames), module) +
                        findOpenApiYamlEndpoints(path, listOf(methodNames), module) +
                        findMockMvcEndpointUsage(path, listOf(methodNames), module) +
                        findWebTestClientEndpointUsage(path, methodNames, module)
            })
            .setTargetRenderer { SpringWebUtil.getTargetRenderer() }
            .setTooltipText(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.tooltip"))
            .setPopupTitle(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.popup"))
            .setEmptyPopupText(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.empty"))
            .createLineMarkerInfo(element)
    }

    private fun getRouteFunction(element: PsiElement): EndpointParameter? {
        val identifier = element as? PsiIdentifier ?: return null
        val methodName = identifier.text ?: return null
        if (methodName !in SpringWebClasses.URI_TYPE) return null

        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return null
        if (!method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        val psiClass = method.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return null

        val psiMethod = element.parentOfType<PsiMethodCallExpression>() ?: return null
        val path = getPathFromRouteFunction(psiMethod.toUElement())
        if (path.isEmpty()) return null

        return EndpointParameter(path, methodName)
    }

    private fun getCoRouteFunction(uParent: UElement?): EndpointParameter? {
        val uElement = uParent as? UCallExpression ?: return null

        val methodName = uElement.methodName ?: return null
        if (methodName !in SpringWebClasses.URI_TYPE) return null

        val method = findContainingMethod(uElement) ?: return null
        if (!method.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        val psiClass = method.javaPsi.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return null

        val path = SpringWebUtil.getPathFromCallExpression(uElement)
        if (path.isEmpty()) return null

        return EndpointParameter(path, methodName)
    }

    private fun getPathFromRouteFunction(uMethod: UElement?): String {
        var path = ""
        uMethod?.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.methodName in SpringWebClasses.URI_TYPE) {
                    val uriArgument = node.valueArguments.getOrNull(0) as? ULiteralExpression
                    path = uriArgument?.value as? String ?: return super.visitCallExpression(node)
                }
                return super.visitCallExpression(node)
            }
        })
        return path
    }

    private fun findContainingMethod(expression: UElement): UMethod? {
        var currentExpression: UElement? = expression
        while (currentExpression != null && currentExpression !is UMethod) {
            currentExpression = currentExpression.uastParent
        }
        return currentExpression as? UMethod
    }

    data class EndpointParameter(
        val path: String,
        val methodNames: String
    )

}