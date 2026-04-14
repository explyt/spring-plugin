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

class EnvironmentalRemoveFileAction(
    private val envDataHolder: EnvDataHolder,
) : AnAction() {
    init {
        templatePresentation.icon = AllIcons.General.Remove
        templatePresentation.text = "Remove Environmental File"
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = !envDataHolder.envFiles.selected.isNullOrEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        envDataHolder.removeFile(project)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}