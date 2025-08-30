/*
 * Copyright © 2025 Explyt Ltd
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
