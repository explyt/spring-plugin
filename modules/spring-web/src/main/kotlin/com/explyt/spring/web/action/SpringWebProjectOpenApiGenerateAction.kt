/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.action

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.spring.web.editor.openapi.OpenApiUtils.isAbsolutePath
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.OpenApiFileUtil
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType
import java.util.concurrent.Callable

class SpringWebProjectOpenApiGenerateAction :
    AnAction({ "Generate OpenApi Specification" }, AllIcons.RunConfigurations.TestState.Run) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ReadAction.nonBlocking(Callable { collectSpecification(project) })
            .inSmartMode(project)
            .expireWhen { project.isDisposed }
            .finishOnUiThread(ModalityState.nonModal()) { generate(it, project) }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun collectSpecification(project: Project): ProjectSpecification {
        val endpointInfos = collectEndpointInfos(project)
        return ProjectSpecification(endpointInfos, OpenApiFileUtil.INSTANCE.endpointServersOf(endpointInfos))
    }

    private fun collectEndpointInfos(project: Project): List<EndpointInfo> {
        val endpointElements = project.modules.flatMapTo(mutableListOf()) { module ->
            ProgressManager.checkCanceled()
            SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module, listOf(EndpointType.SPRING_MVC))
        }
        val uClasses = endpointElements.asSequence()
            .mapNotNull { it.containingClass }
            .mapNotNull { it.toUElementOfType<UClass>() }
            .toCollection(mutableSetOf())

        val endpointInfos = mutableListOf<EndpointInfo>()

        for (uClass in uClasses) {
            ProgressManager.checkCanceled()
            val psiClass = uClass.javaPsi

            val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return endpointInfos

            val isController = psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)
            val isFeignClient = psiClass.isMetaAnnotatedBy(SpringWebClasses.FEIGN_CLIENT)
            if (!isController && !isFeignClient) return endpointInfos
            val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

            val prefixes = HandlerMethods.requestMappingPrefixes(psiClass, requestMappingMah)

            val prefix = prefixes.firstOrNull { !isAbsolutePath(it) } ?: ""
            endpointInfos += uClass.methods
                .mapNotNull { SpringWebUtil.getEndpointInfo(it, prefix) }
                .filter { !isAbsolutePath(it.path) }
        }
        return endpointInfos
    }

    private fun generate(specification: ProjectSpecification, project: Project) {
        StatisticService.getInstance().addActionUsage(StatisticActionId.ACTION_OPENAPI_PROJECT_GENERATE_SPEC)

        OpenApiFileUtil.INSTANCE.createAndShow(
            project,
            specification.endpointInfos,
            emptyList(),
            TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW,
            specification.endpointServers
        )
    }

    private class ProjectSpecification(val endpointInfos: List<EndpointInfo>, val endpointServers: List<String>)

}
