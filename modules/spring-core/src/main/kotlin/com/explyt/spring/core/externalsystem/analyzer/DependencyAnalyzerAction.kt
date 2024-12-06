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

package com.explyt.spring.core.externalsystem.analyzer

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
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

    override fun update(e: AnActionEvent) {
        super.update(e)
        templatePresentation.text = SpringCoreBundle.message("explyt.external.bean.analyzer")
    }

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