package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.externalsystem.utils.Constants
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.projectImport.ProjectOpenProcessor

class SpringBootProjectOpenProcessor : ProjectOpenProcessor() {
    private val importProvider = SpringBootOpenProjectProvider()

    override fun canOpenProject(file: VirtualFile): Boolean = importProvider.canOpenProject(file)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean
    ): Project? {
        return runWithModalProgressBlocking(ModalTaskOwner.guess(), "") {
            importProvider.openProject(virtualFile, projectToClose, forceOpenInNewFrame)
        }
    }

    override val name = Constants.SYSTEM_ID.readableName

    override val icon = SpringIcons.Spring

    override fun canImportProjectAfterwards(): Boolean = true

    override fun importProjectAfterwards(project: Project, file: VirtualFile) {
        importProvider.linkToExistingProject(file, project)
    }
}