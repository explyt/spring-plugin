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

package com.explyt.spring.web.httpclient.action

import com.explyt.spring.web.httpclient.EnvDataHolder
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.vfs.LocalFileSystem

class EnvironmentalAddFileAction(
    private val envDataHolder: EnvDataHolder,
) : AnAction() {
    init {
        templatePresentation.icon = AllIcons.General.Add
        templatePresentation.text = "Add Environmental File"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        val chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null)
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: LocalFileSystem.getInstance().findFileByPath(project.basePath!!)
        val file = chooser.choose(project, currentFile).takeIf { it.size == 1 }?.first() ?: return

        envDataHolder.addFile(file, project)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}