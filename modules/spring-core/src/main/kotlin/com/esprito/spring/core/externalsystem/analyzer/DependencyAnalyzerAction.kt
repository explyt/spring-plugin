package com.esprito.spring.core.externalsystem.analyzer

import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.AbstractDependencyAnalyzerAction
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerAction
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.view.ExternalSystemNode
import com.intellij.openapi.externalSystem.view.ProjectNode
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.vfs.LocalFileSystem

class ToolbarDependencyAnalyzerAction : DependencyAnalyzerAction() {

    private val viewAction = ViewDependencyAnalyzerAction()

    override fun getSystemId(e: AnActionEvent) = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID)

    override fun isEnabledAndVisible(e: AnActionEvent): Boolean {
        return getSystemId(e) == SYSTEM_ID
    }

    override fun setSelectedState(view: DependencyAnalyzerView, e: AnActionEvent) {
        viewAction.setSelectedState(view, e)
    }
}

class ViewDependencyAnalyzerAction : AbstractDependencyAnalyzerAction<ExternalSystemNode<*>>() {

    override fun getSystemId(e: AnActionEvent) = SYSTEM_ID

    override fun getSelectedData(e: AnActionEvent): ExternalSystemNode<*>? {
        return e.getData(ExternalSystemDataKeys.SELECTED_NODES)?.firstOrNull()
    }

    override fun getModule(e: AnActionEvent, selectedData: ExternalSystemNode<*>): Module? {
        val project = e.project ?: return null
        val projectData = selectedData.findParent(ProjectNode::class.java)?.data ?: return null
        val virtualFile = LocalFileSystem.getInstance()
            .findFileByPath(projectData.linkedExternalProjectPath) ?: return null
        return ModuleUtilCore.findModuleForFile(virtualFile, project)
    }

    override fun getDependencyData(e: AnActionEvent, selectedData: ExternalSystemNode<*>) = null

    override fun getDependencyScope(e: AnActionEvent, selectedData: ExternalSystemNode<*>) = null
}