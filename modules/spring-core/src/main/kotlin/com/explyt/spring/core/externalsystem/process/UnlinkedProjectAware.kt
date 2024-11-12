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

package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.NativeSettings
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils.getVirtualFile
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.projectImport.ProjectOpenProcessor
import com.intellij.psi.PsiManager


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

    override fun linkAndLoadProject(project: Project, externalProjectPath: String) {
        val virtualFile = getVirtualFile(externalProjectPath)
        ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtensionOrFail(SpringBootProjectOpenProcessor::class.java)
            .importProjectAfterwards(project, virtualFile)
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