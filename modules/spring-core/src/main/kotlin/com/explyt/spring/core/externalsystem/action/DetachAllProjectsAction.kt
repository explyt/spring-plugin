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
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CancellationException

private val logger = logger<DetachAllProjectsAction>()

private const val DETACH_ACTION_CLASS = "com.intellij.openapi.externalSystem.action.DetachExternalProjectAction"

private const val DETACH_METHOD_NAME = "detachProject"

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
                detachProjectNode(projectData, project)
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

        //the reflected signature is pinned by UnlinkProjectAwareTest.detachProjectSignatureIsStable
        fun detachProjectNode(projectData: ProjectData, project: Project) {
            val method = try {
                Class.forName(DETACH_ACTION_CLASS).declaredMethods.firstOrNull { it.name == DETACH_METHOD_NAME }
            } catch (e: ClassNotFoundException) {
                logger.warn("Explyt detach project: platform class $DETACH_ACTION_CLASS is missing", e)
                return
            }
            if (method == null) {
                logger.warn("Explyt detach project: no '$DETACH_METHOD_NAME' method in $DETACH_ACTION_CLASS")
                return
            }
            try {
                method.invoke(null, project, projectData.owner, projectData, null)
            } catch (e: Exception) {
                // Reflection wraps whatever detachProject threw, so cancellation only becomes visible after unwrapping.
                val thrown = (e as? InvocationTargetException)?.targetException ?: e
                if (thrown is CancellationException) throw thrown
                logger.warn("Explyt detach project: $DETACH_ACTION_CLASS#$DETACH_METHOD_NAME failed", thrown)
                // The linked path identifies the user's machine and projects: keep it out of idea.log,
                // which users routinely attach to public bug reports.
                logger.debug { "Explyt detach project: failed for path ${projectData.linkedExternalProjectPath}" }
            }
        }
    }
}
