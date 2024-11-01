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

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
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