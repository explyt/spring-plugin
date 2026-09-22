/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.messaging.MessageMappingEndpointLoader
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.spring.core.service.beans.ScopedBeanRecord
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMember
import com.intellij.psi.util.InheritanceUtil

/**
 * The beans of one application, for the MCP listing.
 *
 * Reads the same scoped snapshot the bean lookup tool answers from, so both tools describe one chosen
 * application rather than two differently-scoped estimates of it. The listing keeps its own flat shape: one row
 * per name a bean answers to, because that is the enumeration its callers have always received.
 *
 * Must run under a read action.
 */
@Service(Service.Level.PROJECT)
class McpBeanSearchService(private val project: Project) {

    fun getProjectBeansMcp(
        application: PsiClass,
        source: BeanSourcePreference,
        contextId: String?
    ): List<SpringBean> {
        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, source, contextId)
        val mappingClasses = ModuleUtilCore.findModuleForPsiElement(application)
            ?.let { MessageMappingEndpointLoader.searchMessageMappingClasses(it, it.moduleWithDependenciesScope) }
            ?: emptyList()

        return snapshot.records.asSequence()
            .onEach { ProgressManager.checkCanceled() }
            .flatMap { record -> record.rows(mappingClasses) }
            .toList()
    }

    /**
     * One row per known name.
     *
     * A bean declared as `@Bean(name = {"systemClock", "utcClock"})` has always appeared in this listing under
     * each of its names; the lookup tool counts it as one identity instead. Both are right for their own
     * question, so the two enumerations are kept deliberately different rather than silently unified.
     */
    private fun ScopedBeanRecord.rows(mappingClasses: Collection<PsiClass>): Sequence<SpringBean> {
        val className = typeName ?: return emptySequence()
        val type = beanType(mappingClasses)
        val module = declarationModule ?: declaration?.projectModule() ?: ""
        return knownNames.ifEmpty { setOf(name) }.asSequence()
            .map { SpringBean(it, className, type, module) }
    }

    private fun PsiMember.projectModule(): String? =
        takeIf { it.isValid }?.let { ModuleUtilCore.findModuleForPsiElement(it) }?.name

    /**
     * The stereotype of a bean, read from the bean's own type.
     *
     * The type is what carries the stereotype, not the factory that produced it: a `@Bean Clock` declared inside
     * an `@Configuration` is not itself a configuration, so classifying by the declaring class would move every
     * factory-made bean into its factory's category.
     *
     * A native context can report a bean the project has no source for; its exported role is then the only
     * evidence available. An unrecognised role falls back to `COMPONENT`, the listing's long-standing bucket for
     * "other beans" - it claims no particular annotation.
     */
    private fun ScopedBeanRecord.beanType(mappingClasses: Collection<PsiClass>): McpBeanTypes {
        val typeClass = (declaredType as? PsiClassType)?.resolve()
            ?: declaration as? PsiClass
        return typeClass?.takeIf { it.isValid }?.let { getBeanType(it, mappingClasses) }
            ?: runtimeRole?.let(::roleAsBeanType)
            ?: McpBeanTypes.COMPONENT
    }

    private fun roleAsBeanType(role: String): McpBeanTypes? = when (role) {
        "CONTROLLER" -> McpBeanTypes.CONTROLLER
        "REPOSITORY" -> McpBeanTypes.REPOSITORY
        "AUTO_CONFIGURATION" -> McpBeanTypes.AUTO_CONFIGURATION
        "CONFIGURATION_PROPERTIES" -> McpBeanTypes.CONFIGURATION_PROPERTIES
        "CONFIGURATION" -> McpBeanTypes.CONFIGURATION
        "MESSAGE_MAPPING" -> McpBeanTypes.MESSAGE_MAPPING
        "ASPECT" -> McpBeanTypes.ASPECT
        else -> null
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
