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
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManagerImpl
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
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
        val project = e.project ?: return
        val projectsNode = ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
            .mapNotNull { it.externalProjectStructure }
        for (projectNode in projectsNode) {
            try {
                detachProject(project, SYSTEM_ID, projectNode.data)
            } catch (ignore: Exception) {
            }
        }
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, SYSTEM_ID)
    }

    companion object {

        fun detachProject(
            project: Project,
            projectSystemId: ProjectSystemId,
            projectData: ProjectData,
        ) {
            val externalProjectPath = projectData.linkedExternalProjectPath

            try {
                val localSettings = ExternalSystemApiUtil
                    .getLocalSettings<AbstractExternalSystemLocalSettings<*>>(project, projectSystemId)
                localSettings.forgetExternalProjects(setOf(externalProjectPath))
            } catch (ignore: Exception) {
            }

            try {
                val settings = ExternalSystemApiUtil.getSettings(project, projectSystemId)
                settings.unlinkExternalProject(externalProjectPath)
            } catch (ignore: Exception) {
            }

            try {
                val externalProjectsManager = ExternalProjectsManagerImpl.getInstance(project)
                externalProjectsManager.forgetExternalProjectData(projectSystemId, externalProjectPath)
            } catch (ignore: Exception) {
            }

            val orphanModules = collectExternalSystemModules(project, projectSystemId, externalProjectPath)
            if (orphanModules.isNotEmpty()) {
                ProjectDataManagerImpl.getInstance().removeData(
                    ProjectKeys.MODULE, orphanModules, emptyList(), projectData, project, false
                )
            }
        }

        private fun collectExternalSystemModules(
            project: Project, externalSystemId: ProjectSystemId, externalProjectPath: String
        ): List<Module> {
            val result: MutableList<Module> = ArrayList()
            for (module in ModuleManager.getInstance(project).modules) {
                if (ExternalSystemApiUtil.isExternalSystemAwareModule(externalSystemId, module)) {
                    val path = ExternalSystemApiUtil.getExternalRootProjectPath(module)
                    if (externalProjectPath == path) {
                        result.add(module)
                    }
                }
            }
            return result
        }
    }
}