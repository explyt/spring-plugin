/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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