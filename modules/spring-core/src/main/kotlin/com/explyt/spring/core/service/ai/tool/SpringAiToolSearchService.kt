/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.core.service.ai.tool

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.service.NativeSearchService
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.util.runReadNonBlocking
import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import kotlin.io.path.absolutePathString

@Service(Service.Level.PROJECT)
class SpringAiToolSearchService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): SpringAiToolSearchService = project.service()
    }

    fun getProjectInfoJson(): String {
        val propertyFiles = getApplicationPropertyFiles()
        val springProject = runReadNonBlocking {
            val projectBeans = if (SpringSearchServiceFacade.isExternalProjectExist(project)) {
                NativeSearchService.getInstance(project).getProjectBeans()
            } else {
                SpringSearchService.getInstance(project).getProjectBeans()
            }
            val aiToolBeans = mapToAI(projectBeans)
            AiToolSpringProject(aiToolBeans, propertyFiles)
        }
        return Gson().toJson(springProject)
    }

    private fun getApplicationPropertyFiles(): List<String> {
        return runReadNonBlocking {
            DefinedConfigurationPropertiesSearch.getInstance(project).searchPropertyFiles()
                .mapNotNull { it.virtualFile?.toNioPathOrNull()?.absolutePathString() }
        }
    }

    private fun mapToAI(projectBeans: Collection<PsiBean>): List<AiToolBean> {
        return projectBeans.mapNotNull {
            val qualifiedName = it.psiClass.qualifiedName ?: return@mapNotNull null
            val classPath = it.psiClass.containingFile?.virtualFile?.toNioPathOrNull()?.absolutePathString()
                ?: return@mapNotNull null
            AiToolBean(qualifiedName, classPath)
        }
    }

}

data class AiToolSpringProject(val beans: List<AiToolBean>, val applicationPropertyFiles: List<String>)

data class AiToolBean(val className: String, val filePath: String)
