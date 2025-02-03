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

package com.explyt.spring.web.httpclient

import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationConfigurable
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.web.SpringWebBundle
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