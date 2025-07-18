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

package com.explyt.spring.core.service

import com.explyt.spring.core.externalsystem.model.BeanSearch
import com.explyt.spring.core.externalsystem.setting.NativeProjectSettings
import com.explyt.spring.core.externalsystem.utils.Constants
import com.explyt.spring.core.externalsystem.utils.Constants.SYSTEM_ID
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.xdebugger.XDebuggerManager

@Service(Service.Level.PROJECT)
class SpringSearchServiceFacade(private val project: Project) {
    private val springSearchService = SpringSearchService.getInstance(project)
    private val nativeSearchService = NativeSearchService.getInstance(project)

    fun getAllBeansClassesConsideringContext(project: Project): SpringSearchService.FoundBeans {
        return springSearchService.getAllBeansClassesConsideringContext(project)
    }


    fun getAllActiveBeans(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllActiveBeans()
        } else {
            springSearchService.getActiveBeansClasses(module)
        }
    }

    fun getAllActiveBeansLight(module: Module): Set<PsiClass> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeanClasses()
        } else {
            springSearchService.getAllActiveBeansLight(module)
        }
    }

    fun getComponentBeanPsiMethods(module: Module): Set<PsiMethod> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getBeanPsiMethods()
        } else {
            springSearchService.getComponentBeanPsiMethods(module)
        }
    }

    fun searchArrayComponentPsiClassesByBeanMethods(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.searchArrayPsiClassesByBeanMethods()
        } else {
            springSearchService.searchArrayComponentPsiClassesByBeanMethods(module)
        }
    }

    fun getAllBeanByNames(module: Module): Map<String, List<PsiBean>> {
        return if (isExternalProjectExist(project)) {
            nativeSearchService.getAllBeanByNames()
        } else {
            springSearchService.getAllBeanByNames(module)
        }
    }

    fun getExcludedBeansClasses(module: Module): Set<PsiBean> {
        return if (isExternalProjectExist(project)) {
            emptySet()
        } else {
            springSearchService.getExcludedBeansClasses(module)
        }
    }

    fun findActiveBeanDeclarations(
        module: Module,
        byBeanName: String,
        language: Language,
        byBeanPsiType: PsiType? = null,
        qualifier: PsiAnnotation? = null
    ): List<PsiMember> {
        return if (isExternalProjectExist(project)) {
            val beans = nativeSearchService.getAllActiveBeans()
            nativeSearchService.findActiveBeanDeclarations(beans, byBeanName, language, byBeanPsiType, qualifier)
        } else {
            springSearchService.findActiveBeanDeclarations(module, byBeanName, language, byBeanPsiType, qualifier)
        }
    }

    companion object {
        fun getInstance(project: Project): SpringSearchServiceFacade = project.service()

        fun isExternalProjectExist(project: Project): Boolean {
            val debugSession = XDebuggerManager.getInstance(project).currentSession
            if (debugSession != null) {
                val debugProjectSettings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
                    .getLinkedProjectSettings(Constants.DEBUG_SESSION_NAME) as? NativeProjectSettings
                return debugProjectSettings != null
                        && debugProjectSettings.runConfigurationId != null
                        && debugSession.sessionName.isNotBlank()
                        && debugProjectSettings.runConfigurationId!!.contains(debugSession.sessionName)
            }

            if (isUnitTest(project)) return false
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                CachedValueProvider.Result(
                    isExternalProjectExistInternal(project),
                    ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
                )
            }
        }

        fun isUnitTest(project: Project): Boolean {
            val file = FileEditorManager.getInstance(project).selectedEditor?.file ?: return false
            val fileIndex = ProjectRootManager.getInstance(project).fileIndex
            return fileIndex.isInTestSourceContent(file)
        }

        private fun isExternalProjectExistInternal(project: Project): Boolean {
            val firstOrNull = ProjectDataManager.getInstance()
                .getExternalProjectsData(project, SYSTEM_ID).asSequence()
                .mapNotNull { it.externalProjectStructure }
                .filter { !it.isIgnored }
                .filter { it.data.externalName != Constants.DEBUG_SESSION_NAME }
                .firstOrNull { it.children.any { node -> (node.data as? BeanSearch)?.enabled == true } }
            return firstOrNull != null
        }
    }
}