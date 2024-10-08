package com.esprito.spring.core.externalsystem.process

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.externalsystem.utils.Constants
import com.esprito.spring.core.externalsystem.utils.NativeBootUtils.getVirtualFile
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl.setupCreatedProject
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.packaging.artifacts.ModifiableArtifactModel
import com.intellij.projectImport.ProjectImportBuilder
import com.intellij.projectImport.ProjectOpenProcessor
import javax.swing.Icon

class LinkSpringBootProjectImportBuilder : ProjectImportBuilder<Any>() {
    override fun getName(): String = Constants.SPRING_BOOT_NATIVE_ID

    override fun getIcon(): Icon = SpringIcons.Spring

    override fun getList(): List<Any> = emptyList()

    override fun isMarked(element: Any): Boolean = true

    override fun setOpenProjectSettingsAfter(on: Boolean) {}

    override fun createProject(name: String?, path: String): Project? {
        return setupCreatedProject(super.createProject(name, path))?.also {
            it.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
        }
    }

    override fun validate(currentProject: Project?, project: Project) = true

    override fun commit(
        project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?,
        artifactModel: ModifiableArtifactModel?
    ): List<Module> {
        getProjectOpenProcessor().importProjectAfterwards(project, getVirtualFile(fileToImport))
        return emptyList()
    }

    private fun getProjectOpenProcessor() =
        ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtensionOrFail(SpringBootProjectOpenProcessor::class.java)
}