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

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.PackageScanService
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.idea.base.util.projectScope


class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("get_all_spring_boot_applications")
    @McpDescription(description = "Return all SpringBootApplications - fully-qualified (e.g. 'java.util.List') java class names in project")
    suspend fun getAllSpringBootApplications(): SpringBootApplications {
        // val project = currentCoroutineContext().project
        return withContext(Dispatchers.IO) {
            val project = getCurrentProject() ?: mcpFail("project not found")
            val classes = readAction {
                println("!!!1 ${project.basePath}")
                val springBootAppAnnotations = PackageScanService.getInstance(project).getSpringBootAppAnnotations()
                val mapNotNull = springBootAppAnnotations.mapNotNull { it.qualifiedName }
                println("!!!2 $mapNotNull")
                val map = springBootAppAnnotations.asSequence()
                    .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, project.projectScope()) }
                    .mapNotNull { it.qualifiedName }
                    .distinct()
                    .toList()
                //.map { SpringBootApplication(it) }
                println("!!!3 $map")
                map
            }
            SpringBootApplications("f")
        }
    }

    @McpTool("get_project_beans_by_spring_boot_application")
    @McpDescription(description = "Return all project Spring Beans in SpringBootApplication by bean type")
    suspend fun applicationBeans(
        @McpDescription("Fully-qualified class name for SpringBootApplication.")
        applicationClassName: String,
        @McpDescription(
            "Bean Type. Possible values: " +
                    "ASPECT - for org.aspectj.lang.annotation.Aspect, \n" +
                    "MESSAGE_MAPPING - for KafkaListener/RabbitListener and other inheritor of org.springframework.messaging.handler.annotation.MessageMapping, \n" +
                    "CONTROLLER - for org.springframework.stereotype.Controller and inheritors , \n" +
                    "AUTO_CONFIGURATION - for org.springframework.boot.autoconfigure.AutoConfiguration and inheritors , \n" +
                    "CONFIGURATION_PROPERTIES - for org.springframework.boot.context.properties.ConfigurationProperties and inheritors , \n" +
                    "CONFIGURATION - for org.springframework.context.annotation.Configuration and inheritors , \n" +
                    "REPOSITORY - for Spring Data Repositories and org.springframework.stereotype.Repository , \n" +
                    "COMPONENT - for Spring Components/Service and other beans. \n"
        )
        beanType: String,
    ): List<McpSpringBean> {
        val mcpBeanType = getMcpBeanType(beanType) ?: mcpFail("bean type not found $beanType")
        val springBeans = withContext(Dispatchers.IO) {
            val project = currentCoroutineContext().project
            readAction {
                println("!!! project found")
                val applicationPsiClass = JavaPsiFacade.getInstance(project)
                    .findClass(applicationClassName, project.projectScope())
                    ?: mcpFail("Spring Boot Application class not found $applicationClassName")
                println("applicationPsiClass")
                val module = ModuleUtilCore.findModuleForPsiElement(applicationPsiClass)
                    ?: mcpFail("Module not found for $applicationClassName")
                McpBeanSearchService.getInstance(project).getProjectBeansMcp(module)
            }
        }
        return springBeans.asSequence()
            .filter { it.beanType == mcpBeanType }
            .map { McpSpringBean(it.beanName, it.className, it.moduleName) }
            .toList()
    }

    private fun getMcpBeanType(beanType: String): McpBeanTypes? {
        return try {
            McpBeanTypes.valueOf(beanType.uppercase())
        } catch (_: Exception) {
            null
        }
    }

    private fun getCurrentProject(): Project? {
        val openProjects = ProjectManager.getInstance().openProjects
        return openProjects.filter { it.basePath?.contains("spring-boot-4") == true }.firstOrNull { !it.isDefault }
    }
}

@Serializable
data class SpringBootApplications(
    val files: String
)

@Serializable
data class SpringBootApplication(
    @param:McpDescription("fully-qualified java class name for Spring Boot Application Main") val className: String
)

data class McpSpringBean(
    @param:McpDescription("Spring Bean name") val beanName: String,
    @param:McpDescription("full qualified java class name for Spring Bean") val className: String,
    @param:McpDescription("project module name where Spring Bean located") val moduleName: String,
)


