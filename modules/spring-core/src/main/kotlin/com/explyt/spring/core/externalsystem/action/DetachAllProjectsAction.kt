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
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManagerImpl
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import java.util.concurrent.CancellationException

private val logger = logger<DetachAllProjectsAction>()

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
        detachAllProjects(project)
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, SYSTEM_ID)
    }

    companion object {
        fun detachAllProjects(project: Project) {
            // getLinkedProjectsSettings() is a live view over the settings map, and detachProject
            // removes entries from it, so iterate over a snapshot.
            val linkedPaths = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
                .linkedProjectsSettings.mapNotNull { it.externalProjectPath }
            logger.info("Explyt detach all projects: ${linkedPaths.size} linked project(s) to detach")
            for (linkedPath in linkedPaths) {
                detachProject(project, linkedPath)
            }
        }

        fun detachProject(project: Project, externalProjectPath: String) {
            val projectData = ExternalSystemApiUtil.findProjectNode(project, SYSTEM_ID, externalProjectPath)?.data
            if (projectData != null) {
                detachProjectNode(project, SYSTEM_ID, projectData)
                return
            }
            // Import data is absent when a refresh never succeeded or its cache was dropped, and the debug
            // session entry never has it at all. Detaching by node would silently do nothing and leave the
            // link in the settings file forever, so drop the link through the settings, which also
            // publishes onProjectsUnlinked.
            val unlinked = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
                .unlinkExternalProject(externalProjectPath)
            logger.info("Explyt detach project: no import data, unlinked through settings: $unlinked")
            // The linked path identifies the user's machine and projects: keep it out of idea.log,
            // which users routinely attach to public bug reports.
            logger.debug { "Explyt detach project: path $externalProjectPath" }
        }

        fun detachProjectNode(
            project: Project,
            projectSystemId: ProjectSystemId,
            projectData: ProjectData,
        ) {
            val externalProjectPath = projectData.linkedExternalProjectPath

            runLoggingFailure("forget local settings") {
                val localSettings = ExternalSystemApiUtil
                    .getLocalSettings<AbstractExternalSystemLocalSettings<*>>(project, projectSystemId)
                localSettings.forgetExternalProjects(setOf(externalProjectPath))
            }

            runLoggingFailure("unlink settings") {
                val settings = ExternalSystemApiUtil.getSettings(project, projectSystemId)
                settings.unlinkExternalProject(externalProjectPath)
            }

            runLoggingFailure("forget project data") {
                val externalProjectsManager = ExternalProjectsManagerImpl.getInstance(project)
                externalProjectsManager.forgetExternalProjectData(projectSystemId, externalProjectPath)
            }

            runLoggingFailure("remove orphan modules") {
                val orphanModules = collectExternalSystemModules(project, projectSystemId, externalProjectPath)
                if (orphanModules.isNotEmpty()) {
                    ProjectDataManagerImpl.getInstance().removeData(
                        ProjectKeys.MODULE, orphanModules, emptyList(), projectData, project, false
                    )
                }
            }
        }

        private inline fun runLoggingFailure(step: String, action: () -> Unit) {
            try {
                action()
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Explyt detach project: $step failed", e)
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
