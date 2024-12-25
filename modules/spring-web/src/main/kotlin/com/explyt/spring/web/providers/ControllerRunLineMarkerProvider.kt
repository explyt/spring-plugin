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

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getUParentForIdentifier

class ControllerRunLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(psiElement: PsiElement): Info? {
        val uClass = getUParentForIdentifier(psiElement) as? UClass ?: return null
        val psiClass = uClass.javaPsi

        if (!SpringWebUtil.isSpringWebProject(psiElement.project)) return null

        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return null

        val isRequestMapping = psiClass.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)
        val isFeignClient = psiClass.isMetaAnnotatedBy(SpringWebClasses.FEIGN_CLIENT)
        if (!isRequestMapping && !isFeignClient) return null
        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

        val prefixes = if (psiClass.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) {
            requestMappingMah.getAnnotationMemberValues(psiClass, setOf("path", "value"))
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
        } else {
            emptyList()
        }
        val requestMappingServers = prefixes.filter { isAbsolutePath(it) }
        val feignClientServers = if (isFeignClient) {
            val feignClientMah = MetaAnnotationsHolder.of(module, SpringWebClasses.FEIGN_CLIENT)
            feignClientMah.getAnnotationMemberValues(psiClass, setOf("url"))
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
        } else {
            emptyList()
        }

        val prefix = prefixes.firstOrNull { !isAbsolutePath(it) } ?: ""
        val endpointInfos = uClass.methods
            .mapNotNull { SpringWebUtil.getEndpointInfo(it, prefix) }
            .filter { !isAbsolutePath(it.path) }

        return Info(
            RunInSwaggerAction(endpointInfos, requestMappingServers + feignClientServers)
        )
    }

}

