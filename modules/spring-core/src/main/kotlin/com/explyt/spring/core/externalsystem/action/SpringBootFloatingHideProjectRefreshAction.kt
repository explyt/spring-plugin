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

import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.DumbAwareAction

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

        e.presentation.isEnabledAndVisible = SpringSearchServiceFacade.isExternalProjectExist(project)
                && AnnotationTrackerHolderService.getInstance(project).needUpdate()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    init {
        templatePresentation.text = ExternalSystemBundle.message("external.system.reload.notification.action.hide.text")
        templatePresentation.icon = AllIcons.Actions.Close
        templatePresentation.hoveredIcon = AllIcons.Actions.CloseHovered
    }
}