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

import com.explyt.spring.core.externalsystem.model.SpringBeanData
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.externalsystem.utils.NativeBootUtils
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.util.ExplytPsiUtil.onlyAllSupers
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.idea.KotlinLanguage

class BeansDependencyAnalyzerContributor(private val project: Project) : DependencyAnalyzerContributor {

    override fun getDependencies(externalProject: DependencyAnalyzerProject): List<DependencyAnalyzerDependency> {
        StatisticService.getInstance().addActionUsage(StatisticActionId.SPRING_BOOT_PANEL_BEAN_ANALYZER)

        val projectDataNode = ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
            .mapNotNull { it.externalProjectStructure }
            .find { it.data.externalName == externalProject.title } ?: return emptyList()
        val beansNode = ExternalSystemApiUtil.findAll(projectDataNode, SpringBeanData.KEY)
        val parentNode = DADependency(DAModule(externalProject.title), scope("singleton"), null, emptyList())
        val leafsDADependencies = mutableListOf<DADependency>()
        beansNode.asSequence().map { it.data }.forEach { fillDaDependencyMap(it, parentNode, leafsDADependencies) }
        return leafsDADependencies
    }

    override fun getDependencyScopes(externalProject: DependencyAnalyzerProject) = listOf(
        scope("singleton"),
        scope("prototype"),
        scope("session"),
        scope("request"),
        scope("application"),
        scope("websocket"),
    )

    override fun getProjects(): List<DependencyAnalyzerProject> {
        return ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
            .mapNotNull { it.externalProjectStructure?.data }
            .mapNotNull { toDaProject(it) }
    }

    override fun whenDataChanged(listener: () -> Unit, parentDisposable: Disposable) {
    }

    private fun toDaProject(projectData: ProjectData): DAProject? {
        val module = ApplicationManager.getApplication()
            .runReadAction(Computable {
                LocalFileSystem.getInstance().findFileByPath(projectData.linkedExternalProjectPath)
                    ?.let { ModuleUtilCore.findModuleForFile(it, project) }
            }) ?: return null
        return DAProject(module, projectData.externalName)
    }

    private fun fillDaDependencyMap(
        it: SpringBeanData,
        parentNode: DADependency,
        leafsDADependencies: MutableList<DADependency>
    ) {
        val psiClass = ApplicationManager.getApplication()
            .runReadAction(Computable { NativeBootUtils.getBeanTypePsiClass(project, it) }) ?: return
        val scope = scope(it.scope)
        toDaDependency(psiClass, scope, parentNode, leafsDADependencies)
    }

    private fun toDaDependency(
        psiClass: PsiClass,
        scope: DependencyAnalyzerDependency.Scope,
        parentNode: DADependency,
        leafsDADependencies: MutableList<DADependency>
    ) {
        ApplicationManager.getApplication().runReadAction {
            val daArtifact = toDaArtifact(psiClass) ?: return@runReadAction
            val currentDA = DADependency(daArtifact, scope, parentNode, emptyList())
            leafsDADependencies.add(currentDA)
            val supers = psiClass.onlyAllSupers()
            for (psiSuperClass in supers) {
                if (psiSuperClass.qualifiedName == Object::class.java.name) continue
                val superArtifact = getSuperArtifact(psiSuperClass) ?: continue
                val daDependency = DADependency(superArtifact, scope, currentDA, emptyList())
                leafsDADependencies.add(daDependency)
            }
        }
    }

    private fun getSuperArtifact(superClass: PsiClass?): DAArtifact? {
        superClass ?: return null
        return toDaArtifact(superClass)
    }

    private fun toDaArtifact(psiClass: PsiClass): DAArtifact? {
        val name = psiClass.name ?: return null
        val version = getVersion(psiClass) ?: return null
        val groupId = psiClass.qualifiedName?.substringBeforeLast(".$name") ?: return null
        return DAArtifact(groupId, name, version)
    }

    private fun getVersion(psiClass: PsiClass): String? {
        if (psiClass.language == JavaLanguage.INSTANCE) {
            return "java"
        }
        if (psiClass.language == KotlinLanguage.INSTANCE) {
            return "kt"
        }
        return null
    }

    companion object {
        fun scope(name: String) = DAScope(name, StringUtil.toTitleCase(name))
        //private val emptyScope = scope("")
    }
}