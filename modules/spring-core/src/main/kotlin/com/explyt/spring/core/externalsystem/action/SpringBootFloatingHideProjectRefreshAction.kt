/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.xdebugger.XDebuggerManager

class SpringBootFloatingHideProjectRefreshAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_FLOATING_BUTTON_HIDE)
        AnnotationTrackerHolderService.getInstance(project).updateAnnotationTrackerHolder()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        e.presentation.isEnabledAndVisible = XDebuggerManager.getInstance(project).currentSession == null
                && SpringSearchServiceFacade.isExternalProjectExist(project)
                && AnnotationTrackerHolderService.getInstance(project).needUpdate()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    init {
        templatePresentation.text = ExternalSystemBundle.message("external.system.reload.notification.action.hide.text")
        templatePresentation.icon = AllIcons.Actions.Close
        templatePresentation.hoveredIcon = AllIcons.Actions.CloseHovered
    }
}