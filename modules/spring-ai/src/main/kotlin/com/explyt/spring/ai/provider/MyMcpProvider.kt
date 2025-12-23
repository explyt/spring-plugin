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

import com.explyt.spring.core.messaging.MessageMappingEndpointLoader
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.util.projectScope


class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("get_all_spring_boot_applications")
    @McpDescription(description = "Return all SpringBootApplication java class names in project")
    suspend fun getAllSpringBootApplications(): List<SpringBootApplication> {
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

    @McpTool("get_all_beans_by_spring_boot_application")
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
                getProjectBeansMcp(module)
            }
        }
    }
}

private fun getProjectBeansMcp(module: Module): List<SpringBean> {
    return CachedValuesManager.getManager(module.project).getCachedValue(module) {
        CachedValueProvider.Result(
            getProjectBeans(module),
            ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
        )
    }
}

private fun getProjectBeans(module: Module): List<SpringBean> {
    val projectBeans = SpringSearchService.getInstance(module.project).getProjectBeans(module)
    val mappingClasses = MessageMappingEndpointLoader
        .searchMessageMappingClasses(module, module.moduleWithDependenciesScope)
    return projectBeans.asSequence()
        .mapNotNull { toSpringBean(it, mappingClasses) }
        .toList()

}

private fun toSpringBean(bean: PsiBean, mappingClasses: Collection<PsiClass>): SpringBean? {
    val qualifiedName = bean.psiClass.qualifiedName ?: return null
    val module = ModuleUtilCore.findModuleForPsiElement(bean.psiClass) ?: return null
    val beanType = SpringSearchUtils.getBeanType(bean.psiClass, mappingClasses)
    return SpringBean(bean.name, qualifiedName, beanType.name, module.name)
}

data class SpringBootApplication(
    @param:McpDescription("full qualified java class name for Spring Boot Application Main") val className: String
)

data class SpringBean(
    @param:McpDescription("Spring Bean name") val beanName: String,
    @param:McpDescription("full qualified java class name for Spring Bean") val className: String,
    @param:McpDescription("Spring Bean type: Controller, Repository, MessageListener, Service, Aspect") val beanType: String,
    @param:McpDescription("project module name for Spring Bean") val moduleName: String,
)


