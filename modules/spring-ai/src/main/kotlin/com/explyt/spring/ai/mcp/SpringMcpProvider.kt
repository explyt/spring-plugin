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
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.base.util.projectScope


class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("explyt_get_spring_boot_applications")
    @McpDescription(description = "Returns all SpringBootApplications - fully-qualified (e.g. 'java.util.List') Java class names in the project")
    suspend fun getAllSpringBootApplications(
        @McpDescription("Path to the project root")
        projectPath: String
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val applications = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val springBootAppAnnotations = PackageScanService.getInstance(project).getSpringBootAppAnnotations()
                springBootAppAnnotations.asSequence()
                    .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, project.projectScope()) }
                    .distinctBy { it.qualifiedName }
                    .toList()
                    .mapNotNull { toSpringBootApplicationDto(it) }
            }
        }

        return mapper.writeValueAsString(applications)
    }

    @McpTool("explyt_get_project_beans_by_spring_boot_application")
    @McpDescription(description = "Returns all project's Spring Beans in SpringBootApplication by bean type")
    suspend fun applicationBeans(
        @McpDescription("Fully-qualified class name for the SpringBootApplication")
        applicationClassName: String,
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription(
            "Filter results by a Bean Type. Possible values: " +
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
    ): String {
        val project = getCurrentProject(projectPath)
            ?: getCurrentProjectForClass(applicationClassName)
            ?: mcpFail("project not found")
        val mcpBeanType = getMcpBeanType(beanType) ?: mcpFail("bean type not found $beanType")
        val springBeans = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val applicationPsiClass = JavaPsiFacade.getInstance(project)
                    .findClass(applicationClassName, project.projectScope())
                    ?: mcpFail("Spring Boot Application class not found $applicationClassName")
                val module = ModuleUtilCore.findModuleForPsiElement(applicationPsiClass)
                    ?: mcpFail("Module not found for $applicationClassName")
                McpBeanSearchService.getInstance(project).getProjectBeansMcp(module)
            }
        }
        val beans = springBeans.asSequence()
            .filter { it.beanType == mcpBeanType }
            .map { McpSpringBean(it.beanName, it.className, it.moduleName) }
            .toList()
        return mapper.writeValueAsString(beans)
    }

    private fun toSpringBootApplicationDto(psiClass: PsiClass): SpringBootApplicationJson? {
        val qualifiedName = psiClass.qualifiedName ?: return null
        val springBootInfo = SpringBootUtil.getSpringBootStartersInfo(psiClass)
        return SpringBootApplicationJson(
            fullyQualifiedClassName = qualifiedName,
            springBootVersion = SpringBootUtil.getSpringBootVersion(psiClass),
            springBootStarters = springBootInfo?.second ?: emptyList(),
            moduleName = ModuleUtilCore.findModuleForPsiElement(psiClass)?.name,
            buildTool = springBootInfo?.first
        )
    }

    private fun getMcpBeanType(beanType: String): McpBeanTypes? {
        return try {
            McpBeanTypes.valueOf(beanType.uppercase())
        } catch (_: Exception) {
            null
        }
    }

    @McpTool("explyt_find_spring_endpoint")
    @McpDescription(
        description = "Finds endpoints matching a URL pattern across all endpoint types: " +
                "Spring MVC, WebFlux, JAX-RS, HttpExchange, OpenFeign, OpenAPI, message brokers (Kafka/RabbitMQ listeners), " +
                "and event listeners. " +
                "Resolves composed paths from class-level @RequestMapping and method-level @GetMapping/@PostMapping etc. " +
                "Returns matching endpoints with full path, HTTP methods, controller class, method name, parameters, " +
                "return type, file path, line number, and endpoint type. " +
                "Supports partial URL matching (e.g. 'requests' matches '/api/.../requests') " +
                "and path variable wildcards (e.g. '{id}' matches any path variable name)."
    )
    suspend fun findEndpoint(
        @McpDescription(
            "URL pattern to search for. Can be: " +
                    "a full path like '/api/orgs/{orgId}/drilldown/{metricId}/requests', " +
                    "a partial path like '/drilldown/requests' or just 'requests', " +
                    "or a path with wildcards like '/{id}/requests'. " +
                    "Path variables like {orgId} are treated as wildcards matching any segment."
        )
        urlPattern: String,
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription(
            "Optional HTTP method filter: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS. " +
                    "Leave empty to match all methods."
        )
        httpMethod: String = "",
    ): String {
        if (urlPattern.isBlank()) mcpFail("urlPattern must not be empty")
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val normalizedPattern = SpringWebUtil.simplifyUrl(urlPattern)
        val methodFilter = httpMethod.trim().uppercase().takeIf { it.isNotEmpty() }

        val endpoints = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val allEndpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()

                allEndpoints.asSequence()
                    .filter { matchesUrlPattern(it, normalizedPattern) }
                    .filter { methodFilter == null || it.requestMethods.isEmpty() || it.requestMethods.any { m -> m.equals(methodFilter, ignoreCase = true) } }
                    .take(MAX_ENDPOINT_RESULTS)
                    .mapNotNull { toEndpointJson(it, project) }
                    .toList()
            }
        }

        return mapper.writeValueAsString(endpoints)
    }

    private fun matchesUrlPattern(endpoint: EndpointElement, normalizedPattern: String): Boolean {
        val endpointPath = SpringWebUtil.simplifyUrl(endpoint.path)
        // Exact/regex match (handles {param} wildcards)
        if (SpringWebUtil.isEndpointMatches(endpointPath, normalizedPattern)) return true
        // Substring match for partial URLs
        if (endpointPath.contains(normalizedPattern)) return true
        return false
    }

    private fun toEndpointJson(endpoint: EndpointElement, project: Project): EndpointJson? {
        val psiMethod = endpoint.psiElement as? PsiMethod
        val controllerClass = endpoint.containingClass ?: psiMethod?.containingClass
        val basePath = project.basePath?.let { "$it/" }
        val filePath = (endpoint.containingFile ?: endpoint.psiElement.containingFile)?.virtualFile?.path
        val relativePath = if (basePath != null && filePath != null && filePath.startsWith(basePath)) {
            filePath.removePrefix(basePath)
        } else {
            filePath
        }
        val rawLine = endpoint.psiElement.getLineNumber(start = true)
        val lineNumber = if (rawLine >= 0) rawLine + 1 else 1

        val parameters = if (psiMethod != null) extractParameters(psiMethod) else emptyList()
        val returnType = psiMethod?.returnType?.canonicalText

        return EndpointJson(
            httpMethods = endpoint.requestMethods.ifEmpty { listOf("ALL") },
            fullPath = endpoint.path,
            controllerClass = controllerClass?.qualifiedName,
            methodName = psiMethod?.name,
            filePath = relativePath,
            line = lineNumber,
            parameters = parameters,
            returnType = returnType,
            endpointType = endpoint.type.readable,
        )
    }

    private fun extractParameters(psiMethod: PsiMethod): List<EndpointParameterJson> {
        val result = mutableListOf<EndpointParameterJson>()

        for (info in SpringWebUtil.collectPathVariables(psiMethod)) {
            result += EndpointParameterJson(info.name, "PATH", info.typeFqn, info.isRequired)
        }
        for (info in SpringWebUtil.collectRequestParameters(psiMethod)) {
            result += EndpointParameterJson(info.name, "QUERY", info.typeFqn, info.isRequired, info.defaultValue)
        }
        val body = SpringWebUtil.getRequestBodyInfo(psiMethod)
        if (body != null) {
            result += EndpointParameterJson(body.name, "BODY", body.typeFqn, body.isRequired)
        }
        for (info in SpringWebUtil.collectRequestHeaders(psiMethod)) {
            result += EndpointParameterJson(info.name, "HEADER", info.typeFqn, info.isRequired, info.defaultValue)
        }

        return result
    }

    companion object {
        private const val MAX_ENDPOINT_RESULTS = 50
        private val mapper = ObjectMapper()
    }
}

private fun getCurrentProject(projectPath: String?): Project? {
    projectPath ?: return null
    val openProjects = ProjectManager.getInstance().openProjects
        .filter { !it.isDefault }
    if (openProjects.size == 1) return openProjects[0]

    return openProjects.find { it.basePath == projectPath }
}

private suspend fun getCurrentProjectForClass(applicationClassName: String? = null): Project? {
    if (applicationClassName != null) {
        val openProjects = ProjectManager.getInstance().openProjects.filter { !it.isDefault }
        for (project in openProjects) {
            val applicationPsiClass = readAction {
                JavaPsiFacade.getInstance(project).findClass(applicationClassName, project.projectScope())
            }
            if (applicationPsiClass != null) return project
        }
    }
    return null
}

data class SpringBootApplicationJson(
    val fullyQualifiedClassName: String,
    val springBootVersion: String,
    val springBootStarters: List<String>,
    val moduleName: String?,
    val buildTool: String?,
)

data class SpringBootApplication(
    @param:McpDescription("fully-qualified java class name for Spring Boot Application Main") val className: String
)

data class McpSpringBean(
    @param:McpDescription("Spring Bean name") val beanName: String,
    @param:McpDescription("full qualified java class name for Spring Bean") val className: String,
    @param:McpDescription("project module name where Spring Bean located") val moduleName: String,
)

data class EndpointJson(
    val httpMethods: List<String>,
    val fullPath: String,
    val controllerClass: String?,
    val methodName: String?,
    val filePath: String?,
    val line: Int,
    val parameters: List<EndpointParameterJson>,
    val returnType: String?,
    val endpointType: String,
)

data class EndpointParameterJson(
    val name: String,
    val source: String,
    val type: String,
    val required: Boolean,
    val defaultValue: String? = null,
)

