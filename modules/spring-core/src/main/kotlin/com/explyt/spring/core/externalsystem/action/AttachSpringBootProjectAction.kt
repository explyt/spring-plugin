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
            ) ?: return
            val mainFile = mainClass.containingFile?.virtualFile ?: return
            val canonicalPath = mainFile.canonicalPath ?: return
            if (ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).getLinkedProjectSettings(canonicalPath) != null) {
                ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
                    ExternalSystemUtil.refreshProject(canonicalPath, ImportSpecBuilder(project, SYSTEM_ID))
                }
                return
            }

            SpringBootOpenProjectProvider().linkToExistingProject(
                mainFile, selectedRunConfiguration, mainClass.qualifiedName, project
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