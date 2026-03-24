/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.action

import com.explyt.spring.ai.SpringAiBundle.message
import com.explyt.spring.ai.service.AiPluginService
import com.explyt.spring.core.util.ActionUtil
import com.explyt.spring.web.language.http.HttpFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class ConvertCurlToHttpAction : AnAction(message("explyt.spring.ai.action.curl.to.http")) {
    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        e.presentation.isEnabledAndVisible = virtualFile?.fileType == HttpFileType.INSTANCE
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val fileName = virtualFile.name

        val curl = askUser(project)?.trim() ?: return

        val prompt = message("action.prompt.convert.curl", curl, fileName)
        AiPluginService.getInstance(project).performPrompt(prompt, virtualFile)
    }

    private fun askUser(project: Project): String? {
        return Messages.showMultilineInputDialog(
            project, "Curl:", "", null, null, null
        )
    }
}

class ConvertPostmanToHttpAction : AnAction(message("explyt.spring.ai.action.post.to.http")) {
    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile?.fileType == HttpFileType.INSTANCE
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val fileName = virtualFile.name

        val postmanFile = askUser(project, virtualFile) ?: return
        val prompt = message("action.prompt.convert.postman", fileName)
        AiPluginService.getInstance(project).performPrompt(prompt, postmanFile)
    }

    private fun askUser(project: Project, virtualFile: VirtualFile): VirtualFile? {
        return FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFileDescriptor("json"), project, virtualFile
        )
    }
}
