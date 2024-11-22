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
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.MODAL_SYNC
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SpringBootOpenProjectProvider : AbstractOpenProjectProvider() {
    override val systemId: ProjectSystemId = SYSTEM_ID

    override fun isProjectFile(file: VirtualFile) = file.extension == "java" || file.extension == "kt"

    @Deprecated("use async method instead", replaceWith = ReplaceWith("linkToExistingProjectAsync"))
    override fun linkToExistingProject(projectFile: VirtualFile, project: Project) =
        linkToExistingProject(projectFile, null, project)

    fun linkToExistingProject(projectFile: VirtualFile, runConfigurationName: String?, project: Project) {
        ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(true)
        val projectSettings = NativeProjectSettings()
        projectSettings.externalProjectPath = projectFile.canonicalPath
        projectSettings.runConfigurationName = runConfigurationName
        val externalProjectPath = projectSettings.externalProjectPath
        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(projectSettings)

        ExternalSystemUtil.refreshProject(
            externalProjectPath,
            ImportSpecBuilder(project, SYSTEM_ID).usePreviewMode().use(MODAL_SYNC)
        )

        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
            ExternalSystemUtil.refreshProject(
                externalProjectPath,
                ImportSpecBuilder(project, SYSTEM_ID)
            )
        }
    }
}