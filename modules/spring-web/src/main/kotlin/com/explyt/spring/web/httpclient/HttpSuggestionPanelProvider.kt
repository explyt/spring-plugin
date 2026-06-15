/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.explyt.plugin.PluginIds
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationConfigurable
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.web.SpringWebBundle
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

class HttpSuggestionPanelProvider : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        return if (file.name.endsWith(".http") || file.name.endsWith(".rest")) {
            if (ScratchUtil.isScratch(file) && PluginIds.HTTP_CLIENT_JB.isEnabled()) return null
            Function { fileEditor: FileEditor -> getSuggestionPanel(fileEditor) }
        } else null
    }

    private fun getSuggestionPanel(fileEditor: FileEditor): JComponent? {
        if (!SpringToolRunConfigurationConfigurable.shellScriptEnabled()) return null
        if (!SpringToolRunConfigurationsSettingsState.getInstance().httpCliPath.isNullOrEmpty()) return null
        val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Warning)
        panel.text = SpringWebBundle.message("explyt.spring.http.cli.empty")
        panel.createActionLabel(SpringWebBundle.message("explyt.spring.http.cli.empty.setup")) {
            panel.isVisible = false
            ShowSettingsUtil.getInstance().showSettingsDialog(null, Constants.SYSTEM_ID.readableName)
        }
        return panel
    }
}