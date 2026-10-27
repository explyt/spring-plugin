/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
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
        val selectedConfiguration = RunManager.getInstanceIfCreated(project)?.selectedConfiguration?.configuration
        presentation.isVisible = selectedConfiguration.supportsSpringBootAttach(project)
        // Attaching resolves the main class through indexes, so keep the button in place but inert while indexing.
        presentation.isEnabled = presentation.isVisible && !DumbService.isDumb(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        AttachSpringBootProjectAction.attachProject(project)
    }

    private fun RunConfiguration?.supportsSpringBootAttach(project: Project): Boolean {
        // An external-system (Gradle) configuration has no main class to peek at: it qualifies by carrying a task in a
        // project that depends on Spring Boot at all, and the exact module/main-class resolution happens in
        // actionPerformed like for the other types.
        if (this is ExternalSystemRunConfiguration) return settings.taskNames.isNotEmpty() && hasSpringBoot(project)
        val mainClassName = when (this) {
            is ApplicationConfiguration -> mainClassName
            is KotlinRunConfiguration -> mainClassName
            else -> null
        }
        return !mainClassName.isNullOrBlank()
    }

    /**
     * Read from the project's library model, which the platform caches per root change: no PSI, no index. Without it
     * a `clean` or `test` task in any Gradle project enabled the button, only to fail on the click. The BGT update
     * already holds read access, which the lookup requires.
     */
    private fun hasSpringBoot(project: Project): Boolean =
        JavaLibraryUtil.hasLibraryJar(project, SpringCoreUtil.SPRING_BOOT_MAVEN)
}
