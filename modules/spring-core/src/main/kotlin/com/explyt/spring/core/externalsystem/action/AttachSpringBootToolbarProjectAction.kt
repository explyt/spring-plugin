/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringIcons
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration

class AttachSpringBootToolbarProjectAction : DumbAwareAction() {
    init {
        templatePresentation.text = message("explyt.external.project.action.link.text")
        templatePresentation.icon = SpringIcons.SpringExplorer
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    /**
     * Runs on every main-toolbar refresh, so it must stay a cheap in-memory check. Exact main-class resolution is
     * deferred until the action is invoked because PSI and index access here can block toolbar updates for seconds.
     */
    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project ?: return
        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration?.configuration
        presentation.isVisible = selectedConfiguration.supportsSpringBootAttach()
        presentation.isEnabled = presentation.isVisible && !DumbService.isDumb(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        AttachSpringBootProjectAction.attachProject(project)
    }

    private fun RunConfiguration?.supportsSpringBootAttach(): Boolean {
        val mainClassName = when (this) {
            is ApplicationConfiguration -> mainClassName
            is KotlinRunConfiguration -> runClass
            else -> null
        }
        return !mainClassName.isNullOrBlank()
    }
}
