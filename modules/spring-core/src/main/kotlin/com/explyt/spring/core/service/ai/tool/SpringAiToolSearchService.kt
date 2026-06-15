/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
