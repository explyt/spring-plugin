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

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.providers.EndpointUsageSearcher.findMockMvcEndpointUsage
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlEndpoints
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier

class ControllerEndpointLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uParent = getUParentForIdentifier(element)
        if (uParent !is UMethod) return
        val psiMethod = uParent.javaPsi

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) return
        val psiClass = psiMethod.containingClass ?: return
        if (!psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)) return

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val path = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: ""

        val prefix = if (psiClass.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) {
            requestMappingMah.getAnnotationMemberValues(psiClass, setOf("path", "value")).asSequence()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .firstOrNull() ?: ""
        } else {
            ""
        }

        val fullPath = SpringWebUtil.simplifyUrl("$prefix/$path")

        val requestMethods =
            requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("method"))
                .map { it.text.split('.').last() }

        result += NavigationGutterIconBuilder.create(SpringIcons.ReadAccess)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy {
                findOpenApiJsonEndpoints(fullPath, requestMethods, module) +
                        findOpenApiYamlEndpoints(fullPath, requestMethods, module) +
                        findMockMvcEndpointUsage(fullPath, requestMethods, module)
            })
            .setTargetRenderer { SpringWebUtil.getTargetRenderer() }
            .setTooltipText(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.tooltip"))
            .setPopupTitle(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.popup"))
            .setEmptyPopupText(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.empty"))
            .createLineMarkerInfo(element)
    }

}