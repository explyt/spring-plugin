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

import com.explyt.spring.core.externalsystem.DebugProjectResolverPolicy
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.setting.RunConfigurationType
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.MODAL_SYNC
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration

class SpringBootOpenProjectProvider : AbstractOpenProjectProvider() {
    override val systemId: ProjectSystemId = SYSTEM_ID

    override fun isProjectFile(file: VirtualFile) = file.extension == "java" || file.extension == "kt"

    @Deprecated("use async method instead", replaceWith = ReplaceWith("linkToExistingProjectAsync"))
    override fun linkToExistingProject(projectFile: VirtualFile, project: Project) =
        linkToExistingProject(projectFile, null, null, project)

    fun linkToExistingProject(
        projectFile: VirtualFile, runConfiguration: RunConfiguration?, qualifiedMainClassName: String?, project: Project
    ) {
        ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(true)
        val projectSettings = NativeProjectSettings()
        projectSettings.externalProjectPath = projectFile.canonicalPath
        projectSettings.runConfigurationName = runConfiguration?.name

        projectSettings.runConfigurationType = getConfigurationType(runConfiguration)
        projectSettings.qualifiedMainClassName = qualifiedMainClassName
        val externalProjectPath = projectSettings.externalProjectPath
        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(projectSettings)

        if (ApplicationManager.getApplication().isWriteAccessAllowed
            || ApplicationManager.getApplication().isWriteIntentLockAcquired
        ) {
            ExternalSystemUtil.refreshProject(
                externalProjectPath,
                ImportSpecBuilder(project, SYSTEM_ID).usePreviewMode().use(MODAL_SYNC)
            )
        }

        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
            ExternalSystemUtil.refreshProject(
                externalProjectPath,
                ImportSpecBuilder(project, SYSTEM_ID)
            )
        }
    }

    fun attachDebugProject(project: Project, rawBeanData: String, runConfigurationId: String) {
        val canonicalPath = Constants.DEBUG_SESSION_NAME
        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).unlinkExternalProject(canonicalPath)

        val projectSettings = NativeProjectSettings()
        projectSettings.externalProjectPath = canonicalPath
        projectSettings.runConfigurationId = runConfigurationId
        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(projectSettings)

        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
            val importSpecBuilder = ImportSpecBuilder(project, SYSTEM_ID)
                .projectResolverPolicy(DebugProjectResolverPolicy(rawBeanData))
            ExternalSystemUtil.refreshProject(canonicalPath, importSpecBuilder)
        }
    }

    private fun getConfigurationType(runConfiguration: RunConfiguration?): RunConfigurationType {
        return when (runConfiguration) {
            is KotlinRunConfiguration -> RunConfigurationType.KOTLIN
            is SpringBootRunConfiguration -> RunConfigurationType.EXPLYT
            is ApplicationConfiguration -> RunConfigurationType.APPLICATION
            else -> RunConfigurationType.EXPLYT
        }
    }
}