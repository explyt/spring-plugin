/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
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

        val isController = psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)
        val isFeignClient = psiClass.isMetaAnnotatedBy(SpringWebClasses.FEIGN_CLIENT)
        if (!isController && !isFeignClient) return null
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
            AllIcons.RunConfigurations.TestState.Run,
            arrayOf(RunInSwaggerAction(endpointInfos, requestMappingServers + feignClientServers)),
            { SpringWebBundle.message("explyt.web.run.linemarker.swagger.title") }
        )
    }

}

