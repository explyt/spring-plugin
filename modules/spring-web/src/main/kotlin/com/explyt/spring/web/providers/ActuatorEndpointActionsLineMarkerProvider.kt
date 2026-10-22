/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class ActuatorEndpointActionsLineMarkerProvider : LineMarkerProviderDescriptor() {

    override fun getName(): String? = null
    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        result += elements.mapNotNull { getLineMarkerFor(it) }
    }

    private fun getLineMarkerFor(psiElement: PsiElement): LineMarkerInfo<PsiElement>? {
        ProgressManager.checkCanceled()

        val uMethod = getUParentForIdentifier(psiElement) as? UMethod ?: return null
        val psiMethod = uMethod.javaPsi
        if (OPERATIONS.none { psiMethod.isMetaAnnotatedBy(it) }) return null
        val psiClass = psiMethod.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.ACTUATOR_ENDPOINT)) return null

        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null
        val endpoint = SpringWebEndpointsSearcher.getInstance(module.project)
            .getAllEndpoints(module, listOf(EndpointType.ACTUATOR))
            .firstOrNull { it.psiElement.isEquivalentTo(psiMethod) } ?: return null

        return LineMarkerInfo(
            psiElement,
            psiElement.textRange,
            SpringIcons.ReadAccess,
            { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.tooltip") },
            EndpointIconGutterHandler(
                endpointInfoOf(endpoint, psiElement, psiMethod),
                StatisticActionId.GUTTER_ACTUATOR_ENDPOINT_USAGE,
                offersOpenApiDescription = false
            ),
            GutterIconRenderer.Alignment.RIGHT,
            { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.icon.accessible") }
        )
    }

    private fun endpointInfoOf(endpoint: EndpointElement, anchor: PsiElement, method: PsiMethod) = EndpointInfo(
        endpoint.path,
        endpoint.requestMethods,
        anchor,
        method.name,
        endpoint.containingClass?.name.orEmpty(),
        "",
        SpringWebUtil.getTypeFqn(method.returnType, method.language),
        produces = endpoint.produces
    )
}

private val OPERATIONS = listOf(
    SpringCoreClasses.ACTUATOR_READ_OPERATION,
    SpringCoreClasses.ACTUATOR_WRITE_OPERATION,
    SpringCoreClasses.ACTUATOR_DELETE_OPERATION,
)
