/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.spring.web.util.MappingPathPlaceholders
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytUastUtil.getCommentText
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lombok.utils.decapitalize
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class ControllerEndpointActionsLineMarkerProvider : LineMarkerProviderDescriptor() {

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

        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null

        val psiClass = psiMethod.containingClass ?: return null
        if (!psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)) return null
        val mappingSource = SpringWebUtil.requestMappingSourceOf(psiMethod) ?: return null
        val controllerName = psiClass.name ?: return null

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val path = requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: ""
        if (OpenApiUtils.isAbsolutePath(path)) return null

        val prefix = HandlerMethods.requestMappingPrefixes(psiClass, requestMappingMah).firstOrNull() ?: ""
        if (OpenApiUtils.isAbsolutePath(prefix)) return null

        val fullPath = SpringWebUtil.simplifyUrl(MappingPathPlaceholders.resolve(module, "$prefix/$path"))

        val requestMethods =
            requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("method"))
                .map { it.text.split('.').last() }
        val produces = requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("produces"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
        val consumes = requestMappingMah.getAnnotationMemberValues(mappingSource, setOf("consumes"))
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }

        val description = uMethod.comments.firstOrNull()?.getCommentText() ?: ""
        val returnType = uMethod.returnType
        val returnTypeFqn = SpringWebUtil.getTypeFqn(returnType, psiMethod.language)

        val endpointElement = EndpointInfo(
            fullPath,
            requestMethods,
            psiElement,
            uMethod.name,
            controllerName.replace("controller", "", true)
                .decapitalize(),
            description,
            returnTypeFqn,
            SpringWebUtil.collectPathVariables(psiMethod),
            SpringWebUtil.collectRequestParameters(psiMethod),
            SpringWebUtil.getRequestBodyInfo(psiMethod),
            SpringWebUtil.collectRequestHeaders(psiMethod),
            produces,
            consumes
        )

        return LineMarkerInfo(
            psiElement,
            psiElement.textRange,
            SpringIcons.ReadAccess,
            { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.tooltip") },
            EndpointIconGutterHandler(endpointElement),
            GutterIconRenderer.Alignment.RIGHT,
            { SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.icon.accessible") }
        )
    }

}