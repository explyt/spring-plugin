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
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        e.presentation.isEnabledAndVisible = virtualFile.fileType == HttpFileType.INSTANCE
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val fileName = virtualFile.name

        val curl = askUser(project) ?: return

        val prompt = message("action.prompt.convert.curl", curl, fileName)

        //ExternalCallService.getInstance(project).sendPromptWithFiles(prompt, listOf(virtualFile))
    }


    private fun askUser(project: Project): String? {
        return Messages.showMultilineInputDialog(
            project,
            message("action.prompt.convert.curl.dialog.message"),
            "",
            null,
            null,
            null
        )
    }
}

class ConvertPostmanToHttpAction : AnAction(message("explyt.spring.ai.action.post.to.http")) {
    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        e.presentation.isEnabledAndVisible = virtualFile.fileType == HttpFileType.INSTANCE
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val fileName = virtualFile.name

        val postmanFile = askUser(project) ?: return
        val prompt = message("action.prompt.convert.postman", fileName)
        //ExternalCallService.getInstance(project).sendPromptWithFiles(prompt, listOf(postmanFile))
    }

    private fun askUser(project: Project): VirtualFile? {
        return FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFileDescriptor("json"), project, null
        )
    }
}
