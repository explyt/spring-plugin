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

package com.explyt.spring.web.action

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.OpenApiFileUtil
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

class SpringWebProjectOpenApiGenerateAction :
    AnAction({ "Generate OpenApi Specification" }, AllIcons.RunConfigurations.TestState.Run) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val endpointElements = project.modules.flatMapTo(mutableListOf()) { module ->
            SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module, listOf(EndpointType.SPRING_MVC))
        }
        val uClasses = endpointElements.asSequence()
            .mapNotNull { it.containingClass }
            .mapNotNull { it.toUElementOfType<UClass>() }
            .toCollection(mutableSetOf())

        val endpointInfos = mutableListOf<EndpointInfo>()

        for (uClass in uClasses) {
            val psiClass = uClass.javaPsi

            val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return

            val isController = psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)
            val isFeignClient = psiClass.isMetaAnnotatedBy(SpringWebClasses.FEIGN_CLIENT)
            if (!isController && !isFeignClient) return
            val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

            val prefixes = if (psiClass.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) {
                requestMappingMah.getAnnotationMemberValues(psiClass, setOf("path", "value"))
                    .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            } else {
                emptyList()
            }

            val prefix = prefixes.firstOrNull { !isAbsolutePath(it) } ?: ""
            endpointInfos += uClass.methods
                .mapNotNull { SpringWebUtil.getEndpointInfo(it, prefix) }
                .filter { !isAbsolutePath(it.path) }
        }

        generate(endpointInfos, project)
    }

    private fun generate(endpointInfos: List<EndpointInfo>, project: Project) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.ACTION_OPENAPI_PROJECT_GENERATE_SPEC)

        OpenApiFileUtil.INSTANCE.createAndShow(
            project,
            endpointInfos,
            emptyList(),
            TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW
        )
    }

}