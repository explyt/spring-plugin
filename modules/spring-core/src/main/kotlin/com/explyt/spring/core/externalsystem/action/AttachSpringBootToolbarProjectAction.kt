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

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class AttachSpringBootToolbarProjectAction : DumbAwareAction() {
    init {
        templatePresentation.text = message("explyt.external.project.action.link.text")
        templatePresentation.icon = SpringIcons.SpringExplorer
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project ?: return
        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration?.configuration
        presentation.isEnabledAndVisible = !SpringToolRunConfigurationsSettingsState.getInstance().isJavaAgentMode
                && NativeBootUtils.isSupportRunConfiguration(selectedConfiguration)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        AttachSpringBootProjectAction.attachProject(project)
    }
}