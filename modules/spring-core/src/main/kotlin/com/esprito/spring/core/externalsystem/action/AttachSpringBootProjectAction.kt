package com.esprito.spring.core.externalsystem.action

import com.esprito.spring.core.SpringCoreBundle.message
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.externalsystem.process.SpringBootOpenProjectProvider
import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.esprito.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory.WARNING
import com.intellij.openapi.externalSystem.service.notification.NotificationData
import com.intellij.openapi.externalSystem.service.notification.NotificationSource
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable

class AttachSpringBootProjectAction : DumbAwareAction() {
    init {
        templatePresentation.text = message("explyt.external.project.link.text")
        templatePresentation.description = message("explyt.external.project.link.text")
        templatePresentation.icon = SpringIcons.SpringExplorer
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
        val springRunConfiguration = ApplicationManager.getApplication().runReadAction(
            Computable { RunManager.getInstance(project).selectedConfiguration?.configuration }
        ) as? SpringBootRunConfiguration
        if (springRunConfiguration == null) {
            ApplicationManager.getApplication().invokeLater {
                externalSystemNotification(message("explyt.external.project.run.config.required.message"), project)
            }
            return
        }

        val mainFile = ApplicationManager.getApplication().runReadAction(
            Computable { springRunConfiguration.mainClass?.containingFile?.virtualFile }
        ) ?: return
        val canonicalPath = mainFile.canonicalPath ?: return
        if (ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).getLinkedProjectSettings(canonicalPath) != null) {
            ApplicationManager.getApplication().invokeLater {
                val message = message("explyt.external.project.already.linked.message", springRunConfiguration.name)
                externalSystemNotification(message, project)
            }
            return
        }

        SpringBootOpenProjectProvider().linkToExistingProject(mainFile, springRunConfiguration.name, project)
    }

    private fun externalSystemNotification(message: String, project: Project) {
        val notification = NotificationData("", message, WARNING, NotificationSource.TASK_EXECUTION)
        notification.isBalloonNotification = true
        ExternalSystemNotificationManager.getInstance(project).showNotification(SYSTEM_ID, notification)
    }
}