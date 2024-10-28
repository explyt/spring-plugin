package com.esprito.spring.core.externalsystem.action

import com.esprito.spring.core.SpringCoreBundle.message
import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.action.ExternalSystemAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.view.ProjectNode

class ActiveExternalProjectAction : ExternalSystemAction() {
    init {
        templatePresentation.text = message("explyt.external.project.active.text", "External")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectData = getSelectedProjectNode(e)?.data ?: return

        val allProjectDataNodes = ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
            .mapNotNull { it.externalProjectStructure }
        for (dataNode in allProjectDataNodes) {
            dataNode.isIgnored = dataNode.data.linkedExternalProjectPath != projectData.linkedExternalProjectPath
        }
        allProjectDataNodes.forEach {
            ApplicationManager.getApplication().executeOnPooledThread {
                ProjectDataManager.getInstance().importData(it, project)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val projectData = getSelectedProjectNode(e)?.data ?: return
        templatePresentation.text = message("explyt.external.project.active.text", projectData.externalName)
    }

    override fun isVisible(e: AnActionEvent) = getSystemId(e) == SYSTEM_ID

    private fun getSelectedProjectNode(e: AnActionEvent): ProjectNode? {
        val selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES)
        if (selectedNodes == null || selectedNodes.size != 1) return null
        return selectedNodes[0] as? ProjectNode
    }
}