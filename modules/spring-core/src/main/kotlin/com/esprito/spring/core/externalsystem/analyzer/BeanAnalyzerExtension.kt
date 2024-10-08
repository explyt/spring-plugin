package com.esprito.spring.core.externalsystem.analyzer

import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerContributor
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerExtension
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project

class BeanAnalyzerExtension : DependencyAnalyzerExtension {

    override fun createContributor(project: Project, parentDisposable: Disposable): DependencyAnalyzerContributor {
        return BeansDependencyAnalyzerContributor(project)
    }

    override fun isApplicable(systemId: ProjectSystemId): Boolean {
        return systemId == SYSTEM_ID
    }
}