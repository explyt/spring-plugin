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