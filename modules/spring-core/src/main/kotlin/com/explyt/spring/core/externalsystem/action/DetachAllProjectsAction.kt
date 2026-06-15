/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project

class DetachAllProjectsAction : DumbAwareAction() {
    init {
        templatePresentation.text = message("explyt.external.project.detach.all")
        templatePresentation.description = message("explyt.external.project.detach.all")
        templatePresentation.icon = AllIcons.Actions.GC
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
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_REMOVE)
        val project = e.project ?: return
        val projectsNode = ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
            .mapNotNull { it.externalProjectStructure }
        for (projectNode in projectsNode) {
            detachProjectNode(projectNode.data, project)
        }
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, SYSTEM_ID)
    }

    companion object {
        //test for check call UnlinkProjectAwareTest.unlinkProject
        fun detachProjectNode(projectData: ProjectData, project: Project) {
            try {
                val method =
                    Class.forName("com.intellij.openapi.externalSystem.action.DetachExternalProjectAction")
                        .declaredMethods.first { it.name == "detachProject" }
                method.invoke(null, project, projectData.owner, projectData, null)

            } catch (_: Exception) {
            }
        }
    }
}