/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.base.externalSystem.find

class ChangeEnabledDiAction : ExternalSystemNodeAction<BeanSearch>(BeanSearch::class.java) {

    override fun perform(
        project: Project,
        projectSystemId: ProjectSystemId,
        externalData: BeanSearch,
        e: AnActionEvent
    ) {
        externalData.enabled = !externalData.enabled
        val beanSearch = ProjectDataManager.getInstance()
            .getExternalProjectData(project, projectSystemId, externalData.projectPath)
            ?.externalProjectStructure?.find(BeanSearch.KEY)?.data
        beanSearch?.let { it.enabled = externalData.enabled }

        ModificationTrackerManager.getInstance(project).invalidateAll()
        PsiManager.getInstance(project).dropPsiCaches()
        DaemonCodeAnalyzer.getInstance(project).restart()

        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, projectSystemId)
    }
}