/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.action

import com.explyt.spring.ai.SpringAiBundle.message
import com.explyt.spring.ai.service.AiPluginService
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.ActionUtil
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement

class ConvertControllerToOpenapiAction : AnAction(message("explyt.spring.ai.action.controller.to.openapi")) {
    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val enabled = virtualFiles
            .mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            .flatMap { it.classes }
            .any { it.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.CONTROLLER) }

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val controllerPsiClasses = virtualFiles
            .mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            .flatMap { it.classes }
            .filter { it.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.CONTROLLER) }
            .takeIf { it.isNotEmpty() } ?: return

        val controllerNames = controllerPsiClasses.map { it.javaPsi.name }

        val prompt = message("action.prompt.convert.controller", controllerNames)
        AiPluginService.getInstance(project).performPrompt(prompt, virtualFiles.toList())
    }
}

class ConvertOpenapiToControllerAction : AnAction(message("explyt.spring.ai.action.openapi.to.controller")) {
    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val enabled = virtualFiles.any { OpenApiUtils.isOpenApiFile(project, it) }
        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val openApiFiles = virtualFiles.filter { OpenApiUtils.isOpenApiFile(project, it) }
        val fileNames = openApiFiles.joinToString(", ") { it.name }

        val prompt = message("action.prompt.convert.openapi", fileNames)
        AiPluginService.getInstance(project).performPrompt(prompt, openApiFiles.toList())
    }

}
