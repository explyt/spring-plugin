/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.process.SpringBootOpenProjectProvider
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory.WARNING
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.NotificationSource
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.wm.ToolWindowManager

class AttachSpringBootProjectAction : DumbAwareAction() {
    init {
        templatePresentation.text = message("explyt.external.project.link.text")
        templatePresentation.description = message("explyt.external.project.link.text")
        templatePresentation.icon = SpringIcons.SpringBootToolWindow
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val externalSystemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID)
        presentation.isEnabledAndVisible = externalSystemId == SYSTEM_ID
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        attachProject(project)
    }

    companion object {

        /**
         * Must equal the `id` of the `toolWindow` registered in `spring-core-plugin.xml`: the platform looks a tool
         * window up by that exact string and returns `null` for anything else, so a rename on either side would
         * disable the activation silently. `AttachSpringBootProjectActionToolWindowIdTest` pins the two together.
         */
        internal const val TOOL_WINDOW_ID = "Explyt Spring"
        fun attachProject(project: Project) {
            val selectedRunConfiguration = ApplicationManager.getApplication().runReadAction(
                Computable { RunManager.getInstance(project).selectedConfiguration?.configuration }
            )
            if (selectedRunConfiguration == null) {
                ApplicationManager.getApplication().invokeLater {
                    externalSystemNotification(message("explyt.external.project.run.config.required.message"), project)
                }
                return
            }
            attachProject(project, selectedRunConfiguration)
        }

        fun attachProject(project: Project, selectedRunConfiguration: RunConfiguration) {
            StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_ADD)

            val mainClass = ApplicationManager.getApplication().runReadAction(
                Computable { NativeBootUtils.getMainClass(selectedRunConfiguration) }
            )
            val mainFile = mainClass?.containingFile?.virtualFile
            val canonicalPath = mainFile?.canonicalPath
            if (mainClass == null || mainFile == null || canonicalPath == null) {
                // The toolbar action is enabled from a cheap stored-name check, so an unresolvable main class must be
                // reported here instead of failing silently.
                ApplicationManager.getApplication().invokeLater {
                    externalSystemNotification(message("explyt.external.project.run.config.required.message"), project)
                }
                return
            }
            if (ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).getLinkedProjectSettings(canonicalPath) != null) {
                ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
                    ExternalSystemUtil.refreshProject(canonicalPath, ImportSpecBuilder(project, SYSTEM_ID))
                }
                // The action reports success only through the tool window, so an already-linked project has to open it
                // too — otherwise clicking the button looks like nothing happened at all (issue #197).
                activateToolWindow(project)
                return
            }

            SpringBootOpenProjectProvider().linkToExistingProject(
                mainFile, selectedRunConfiguration, mainClass.qualifiedName, project
            )
            activateToolWindow(project)
        }

        /**
         * Brings the Explyt Spring tool window to the front. Not called from the early-return paths above: those
         * already explain themselves with a balloon, and opening an empty tool window on top of it would only add
         * noise.
         */
        private fun activateToolWindow(project: Project) {
            ApplicationManager.getApplication().invokeLater(
                {
                    ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)?.activate(null)
                },
                project.disposed
            )
        }

        fun attachDebugProject(project: Project, rawBeanData: String, runConfigurationId: String) {
            StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_ADD)
            SpringBootOpenProjectProvider().attachDebugProject(project, rawBeanData, runConfigurationId)
        }

        private fun externalSystemNotification(message: String, project: Project) {
            val notification = NotificationData("", message, WARNING, NotificationSource.TASK_EXECUTION)
            notification.isBalloonNotification = true
            ExternalSystemNotificationManager.getInstance(project).showNotification(SYSTEM_ID, notification)
        }
    }
}