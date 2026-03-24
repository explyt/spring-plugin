/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID

class SpringDebugEnabledAction : ToggleAction() {
    init {
        templatePresentation.icon = AllIcons.Toolwindows.ToolWindowDebugger
        templatePresentation.text = SpringCoreBundle.message("explyt.external.project.debug.enable.text")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        val debugMode = SpringToolRunConfigurationsSettingsState.getInstance().isDebugMode
        updateState(debugMode, e.presentation)
        return debugMode
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val settingsState = SpringToolRunConfigurationsSettingsState.getInstance()
        settingsState.isDebugMode = !settingsState.isDebugMode
        updateState(settingsState.isDebugMode, e.presentation)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabledAndVisible = (e.getData(EXTERNAL_SYSTEM_ID) == Constants.SYSTEM_ID)
    }

    private fun updateState(enabled: Boolean, presentation: Presentation) {
        if (enabled) {
            presentation.icon = AllIcons.Toolwindows.ToolWindowDebugger
            presentation.text = SpringCoreBundle.message("explyt.external.project.debug.disable.text")
        } else {
            presentation.icon = AllIcons.General.DebugDisabled
            presentation.text = SpringCoreBundle.message("explyt.external.project.debug.enable.text")
        }
    }
}