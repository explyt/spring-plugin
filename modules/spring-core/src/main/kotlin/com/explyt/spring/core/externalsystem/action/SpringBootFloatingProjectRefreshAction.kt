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

class SpringBootFloatingProjectRefreshAction : DumbAwareAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_FLOATING_BUTTON_REFRESH)
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

        e.presentation.isEnabledAndVisible = SpringSearchServiceFacade.isExternalProjectExist(project)
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