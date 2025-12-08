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

import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.hint.PropertyDebugValueCodeVisionProvider
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.codeVision.CodeVisionHost.LensInvalidateSignal
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

@Order(ExternalSystemConstants.UNORDERED)
class CacheTrackerDataService : AbstractProjectDataService<SpringBeanData, Void>() {
    override fun getTargetDataKey() = SpringBeanData.KEY

    override fun onSuccessImport(
        imported: MutableCollection<DataNode<SpringBeanData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        ApplicationManager.getApplication().invokeLater {
            ModificationTrackerManager.getInstance(project).invalidateAll()
            PsiManager.getInstance(project).dropPsiCaches()
            DaemonCodeAnalyzer.getInstance(project).restart()
            project.service<CodeVisionHost>().invalidateProvider(
                LensInvalidateSignal(null, listOf(PropertyDebugValueCodeVisionProvider.ID))
            )
        }
    }
}