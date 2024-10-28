package com.esprito.spring.core.externalsystem.analyzer

import com.esprito.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.esprito.spring.core.externalsystem.utils.NativeBootUtils
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.DAArtifact
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerGoToAction
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.pom.Navigatable

class BeanGoToAction : DependencyAnalyzerGoToAction(SYSTEM_ID) {

    override fun getNavigatable(e: AnActionEvent): Navigatable? {
        val project = e.project ?: return null
        val dependency = e.getData(DependencyAnalyzerView.DEPENDENCY)?.data as? DAArtifact ?: return null
        return NativeBootUtils.psiClass(dependency, project)
    }
}