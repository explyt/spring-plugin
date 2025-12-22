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

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.messaging.MessageMappingEndpointLoader
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil

@Service(Service.Level.PROJECT)
class McpBeanSearchService {

    fun getProjectBeansMcp(module: Module): List<SpringBean> {
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
        val beanType = getBeanType(bean.psiClass, mappingClasses)
        return SpringBean(bean.name, qualifiedName, beanType, module.name)
    }

    private fun getBeanType(
        psiClass: PsiClass,
        messageMappingClasses: Collection<PsiClass> = emptyList()
    ): McpBeanTypes {
        return if (messageMappingClasses.contains(psiClass)) {
            McpBeanTypes.MESSAGE_MAPPING
        } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONTROLLER)) {
            McpBeanTypes.CONTROLLER
        } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.BOOT_AUTO_CONFIGURATION)) {
            McpBeanTypes.AUTO_CONFIGURATION
        } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) {
            McpBeanTypes.CONFIGURATION_PROPERTIES
        } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.ASPECT)) {
            McpBeanTypes.ASPECT
        } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION)) {
            McpBeanTypes.CONFIGURATION
        } else if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.REPOSITORY)
            || psiClass.isMetaAnnotatedBy("org.springframework.data.repository.RepositoryDefinition")
            || InheritanceUtil.isInheritor(psiClass, "org.springframework.data.repository.Repository")
        ) {
            McpBeanTypes.REPOSITORY
        } else {
            McpBeanTypes.COMPONENT
        }
    }

    companion object {
        fun getInstance(project: Project): McpBeanSearchService = project.service()
    }
}

data class SpringBean(
    val beanName: String, val className: String, val beanType: McpBeanTypes, val moduleName: String,
)

enum class McpBeanTypes {
    ASPECT,
    MESSAGE_MAPPING,
    CONTROLLER,
    AUTO_CONFIGURATION,
    CONFIGURATION_PROPERTIES,
    CONFIGURATION,
    REPOSITORY,
    COMPONENT
}