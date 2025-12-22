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

package com.explyt.spring.ai.provider

import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchService
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.util.projectScope


class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("find all spring boot applications")
    @McpDescription(description = "Return all SpringBootApplication java class names in project")
    suspend fun applications(): List<SpringBootApplication> {
        return withContext(Dispatchers.IO) {
            readAction {
                val project = coroutineContext.project
                val springBootAppAnnotations = PackageScanService.getInstance(project).getSpringBootAppAnnotations()
                springBootAppAnnotations.asSequence()
                    .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, project.projectScope()) }
                    .mapNotNull { it.qualifiedName }
                    .toSet()
                    .map { SpringBootApplication(it) }
            }
        }
    }

    @McpTool("find all beans in spring boot application")
    @McpDescription(description = "Return all project Spring Beans in SpringBootApplication")
    suspend fun applicationBeans(application: SpringBootApplication): List<SpringBean> {
        return withContext(Dispatchers.IO) {
            readAction {
                val project = coroutineContext.project
                val applicationPsiClass = JavaPsiFacade.getInstance(project)
                    .findClass(application.className, project.projectScope())
                    ?: mcpFail("Spring Boot Application class not found ${application.className}")
                val module = ModuleUtilCore.findModuleForPsiElement(applicationPsiClass)
                    ?: mcpFail("Module not found for ${application.className}")
                SpringSearchService.getInstance(project)
                    .getProjectBeanPsiClassesAnnotatedByComponent(module)
                    .mapNotNull {
                        val qualifiedName = it.psiClass.qualifiedName
                        qualifiedName?.let { className -> SpringBean(it.name, className) }
                    }
            }
        }
    }
}

data class SpringBootApplication(
    @param:McpDescription("full qualified java class name") val className: String
)

data class SpringBean(
    @param:McpDescription("Spring Bean name") val beanName: String,
    @param:McpDescription("full qualified java class name for Spring Bean") val className: String
)


/*class McpTool1 : com.intellij.mcpserver.McpTool {
    override suspend fun call(args: JsonObject): McpToolCallResult {

        val project = coroutineContext.project
        mcpFail("fdfdf")
        throwOnFailure()
        McpToolCallResult.text("ff")
        TODO("Not yet implemented")
    }

    override val descriptor: McpToolDescriptor
        get() = TODO("Not yet implemented")
}*/

