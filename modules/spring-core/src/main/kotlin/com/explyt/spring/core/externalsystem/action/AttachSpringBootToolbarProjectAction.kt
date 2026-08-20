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
     * Runs on every main-toolbar refresh, so it must stay a cheap in-memory check: resolving the configuration's main
     * class here means PSI and index access, which produced multi-second `ActionUpdater` warnings. Exact validation
     * happens in [actionPerformed] instead.
     */
    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project ?: return
        val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration?.configuration
        presentation.isVisible = selectedConfiguration.supportsSpringBootAttach()
        // Attaching resolves the main class through indexes, so keep the button in place but inert while indexing.
        presentation.isEnabled = presentation.isVisible && !DumbService.isDumb(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        AttachSpringBootProjectAction.attachProject(project)
    }

    private fun RunConfiguration?.supportsSpringBootAttach(): Boolean {
        val mainClassName = when (this) {
            is ApplicationConfiguration -> mainClassName
            // This platform line exposes the Kotlin entry point as `runClass`; `mainClassName` arrived later.
            is KotlinRunConfiguration -> runClass
            else -> null
        }
        return !mainClassName.isNullOrBlank()
    }
}
