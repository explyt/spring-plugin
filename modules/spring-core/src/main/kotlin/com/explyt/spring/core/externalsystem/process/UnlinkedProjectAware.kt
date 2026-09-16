/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.core.externalsystem.action.DetachAllProjectsAction
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils.getVirtualFile
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = logger<UnlinkedProjectAware>()

class UnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
    override val systemId = Constants.SYSTEM_ID

    override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean {
        if (buildFile.isDirectory) return false
        val mainRootFiles = NativeBootUtils.getMainRootFiles(project)
        return mainRootFiles.contains(buildFile)
    }

    override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean {
        return project.getService(NativeSettings::class.java).getLinkedProjectSettings(externalProjectPath) != null
    }

    override fun subscribe(
        project: Project, listener: ExternalSystemProjectLinkListener, parentDisposable: Disposable
    ) {
        val nativeSettings = project.getService(NativeSettings::class.java)
        nativeSettings.subscribe(UnlinkSettingsListener(project, listener), parentDisposable)
    }

    @Deprecated("use async method instead")
    override fun linkAndLoadProject(project: Project, externalProjectPath: String) {
        val virtualFile = getVirtualFile(externalProjectPath)
        ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtensionOrFail(SpringBootProjectOpenProcessor::class.java)
            .importProjectAfterwards(project, virtualFile)
    }

    override suspend fun linkAndLoadProjectAsync(project: Project, externalProjectPath: String) {
        val virtualFile = getVirtualFile(externalProjectPath)
        ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtensionOrFail(SpringBootProjectOpenProcessor::class.java)
            .importProjectAfterwardsAsync(project, virtualFile)
    }

    override suspend fun unlinkProject(project: Project, externalProjectPath: String) {
        val projectData = ExternalSystemApiUtil.findProjectNode(project, systemId, externalProjectPath)?.data
        withContext(Dispatchers.EDT) {
            if (projectData != null) {
                DetachAllProjectsAction.detachProjectNode(projectData, project)
            } else {
                // Import data is absent when a refresh never succeeded or its cache was dropped. Detaching by
                // node would silently do nothing and leave the link in the settings file forever, so drop the
                // link through the settings, which also publishes onProjectsUnlinked.
                val unlinked = ExternalSystemApiUtil.getSettings(project, systemId)
                    .unlinkExternalProject(externalProjectPath)
                logger.info("Explyt unlink project: no import data, unlinked through settings: $unlinked")
                logger.debug { "Explyt unlink project: path $externalProjectPath" }
            }
            ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, systemId)
        }
    }
}

internal class UnlinkSettingsListener(val project: Project, val listener: ExternalSystemProjectLinkListener) :
    ExternalSystemSettingsListener<NativeProjectSettings> {
    override fun onProjectsLinked(settings: MutableCollection<NativeProjectSettings>) {
        settings.forEach { listener.onProjectLinked(it.externalProjectPath) }
    }

    override fun onProjectsUnlinked(linkedProjectPaths: Set<String>) {
        linkedProjectPaths.forEach { listener.onProjectUnlinked(it) }
        ApplicationManager.getApplication().invokeLater {
            ModificationTrackerManager.getInstance(project).invalidateAll()
            PsiManager.getInstance(project).dropPsiCaches()
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }
}
