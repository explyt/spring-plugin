/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.xdebugger.XDebuggerManager

class SpringBootFloatingProjectRefreshAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_FLOATING_BUTTON_REFRESH)
        AnnotationTrackerHolderService.getInstance(project).updateAnnotationTrackerHolder()
        ExternalSystemUtil.refreshProjects(ImportSpecBuilder(project, SYSTEM_ID))
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val productName = ApplicationNamesInfo.getInstance().fullProductName
        e.presentation.text =
            ExternalSystemBundle.message(
                "external.system.reload.notification.action.reload.text",
                SYSTEM_ID.readableName
            )
        e.presentation.description = ExternalSystemBundle.message(
            "external.system.reload.notification.action.reload.description",
            SYSTEM_ID.readableName,
            productName
        )
        e.presentation.icon = SpringIcons.SpringUpdate

        e.presentation.isEnabledAndVisible = XDebuggerManager.getInstance(project).currentSession == null
                && SpringSearchServiceFacade.isExternalProjectExist(project)
                && AnnotationTrackerHolderService.getInstance(project).needUpdate()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT


    init {
        val productName = ApplicationNamesInfo.getInstance().fullProductName
        templatePresentation.icon = AllIcons.Actions.BuildLoadChanges
        templatePresentation.text =
            ExternalSystemBundle.message("external.system.reload.notification.action.reload.text.empty")
        templatePresentation.description = ExternalSystemBundle.message(
            "external.system.reload.notification.action.reload.description.empty",
            productName
        )
    }
}