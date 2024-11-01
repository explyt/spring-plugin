package com.explyt.spring.core.externalsystem.process

import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
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
        }
    }
}