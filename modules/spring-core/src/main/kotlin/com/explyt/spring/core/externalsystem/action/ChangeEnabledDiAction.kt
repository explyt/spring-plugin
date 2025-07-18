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

import com.explyt.spring.core.externalsystem.model.BeanSearch
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
        ExternalSystemUtil.scheduleExternalViewStructureUpdate(project, projectSystemId)

        PsiManager.getInstance(project).dropPsiCaches()
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}