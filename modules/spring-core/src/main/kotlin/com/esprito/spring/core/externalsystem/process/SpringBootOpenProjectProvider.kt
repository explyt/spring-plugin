package com.esprito.spring.core.externalsystem.process

import com.esprito.spring.core.externalsystem.setting.NativeProjectSettings
import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
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