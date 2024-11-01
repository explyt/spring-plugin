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

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.CONFIGURATION_PROPERTIES
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

private const val MAX_RECURSION_DEPTH = 10

@Service(Service.Level.PROJECT)
class ConfigurationPropertiesService(private val project: Project) {

    fun getPrefixDataHolder(): ConfigurationPropertyHolder {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                ConfigurationPropertyHolder(getAllConfigPrefix()),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getAllConfigPrefix(): MutableMap<String, String> {
        val rootClassesWithPrefix = getRootConfigurationPropertiesClassesWithPrefix()
        val result = mutableMapOf<String, String>()
        for (prefixData in rootClassesWithPrefix) {
            fillPrefixMap(prefixData, result)
        }
        return result
    }

    private fun getRootConfigurationPropertiesClassesWithPrefix(): List<ConfigurationPropertyClassPrefix> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getRootConfigurationPropertiesClassesWithPrefixInner(),
                ModificationTrackerManager.getInstance(project).getUastAnnotationAndLibraryTracker()
            )
        }
    }

    private fun getRootConfigurationPropertiesClassesWithPrefixInner(): List<ConfigurationPropertyClassPrefix> {
        val configAnnotationPsiClasses = getConfigPropertyAnnotation()
        val configRootClasses = configAnnotationPsiClasses
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, GlobalSearchScope.projectScope(project)) }
            .mapNotNull { toConfigurationPropertyClassPrefix(it) }
        val configRootMethodClasses = configAnnotationPsiClasses
            .flatMap { AnnotatedElementsSearch.searchPsiMethods(it, GlobalSearchScope.projectScope(project)) }
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .mapNotNull { toConfigurationPropertyClassPrefix(it) }
        return configRootClasses + configRootMethodClasses
    }

    private fun toConfigurationPropertyClassPrefix(psiClass: PsiClass): ConfigurationPropertyClassPrefix? {
        val module = ModuleUtil.findModuleForPsiElement(psiClass) ?: return null
        val prefix = getPrefixFromAnnotation(psiClass, module).takeIf { it.isNotEmpty() } ?: return null
        return ConfigurationPropertyClassPrefix(psiClass, prefix)
    }

    private fun toConfigurationPropertyClassPrefix(psiMethod: PsiMethod): ConfigurationPropertyClassPrefix? {
        val psiClass = psiMethod.returnPsiClass ?: return null
        val module = ModuleUtil.findModuleForPsiElement(psiMethod) ?: return null
        val prefix = getPrefixFromAnnotation(psiMethod, module).takeIf { it.isNotEmpty() } ?: return null
        return ConfigurationPropertyClassPrefix(psiClass, prefix)
    }

    private fun fillPrefixMap(
        prefixData: ConfigurationPropertyClassPrefix, prefixByQualifiedName: MutableMap<String, String>, depth: Int = 0
    ) {
        if (depth > MAX_RECURSION_DEPTH) return
        val psiClass = prefixData.psiClass
        val qualifiedName = psiClass.qualifiedName ?: return
        prefixByQualifiedName[qualifiedName] = prefixData.prefix

        val fields = psiClass.fields.takeIf { it.isNotEmpty() } ?: return
        val module = ModuleUtil.findModuleForPsiElement(psiClass) ?: return
        val allActiveBeans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeansLight(module)

        for (field in fields) {
            val resolvedPsiClass = field.type.resolvedPsiClass ?: continue
            if (allActiveBeans.contains(resolvedPsiClass)) continue

            val file = resolvedPsiClass.containingFile.virtualFile
            if (ProjectRootManager.getInstance(project).fileIndex.isInSource(file)) {
                val prefix = prefixData.prefix + "${field.name}."
                val propertyClassPrefix = ConfigurationPropertyClassPrefix(resolvedPsiClass, prefix)
                fillPrefixMap(propertyClassPrefix, prefixByQualifiedName, depth + 1)
            }
        }
    }

    private fun getConfigPropertyAnnotation(): Set<PsiClass> {
        val configPropertyAnnotation = LibraryClassCache.searchForLibraryClass(project, CONFIGURATION_PROPERTIES)
            ?: return emptySet()
        val childrenConfigPropertyAnnotation = MetaAnnotationUtil
            .getChildren(configPropertyAnnotation, GlobalSearchScope.allScope(project))
        return childrenConfigPropertyAnnotation + configPropertyAnnotation
    }

    companion object {
        fun getInstance(project: Project): ConfigurationPropertiesService = project.service()

        fun getPrefixFromAnnotation(psiMember: PsiMember, module: Module): String {
            val annotationsHolder = SpringSearchService.getInstance(module.project)
                .getMetaAnnotations(module, CONFIGURATION_PROPERTIES)

            val prefix = annotationsHolder.getAnnotationMemberValues(psiMember, setOf("prefix", "value"))
                .asSequence()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .firstOrNull()
            return PropertyUtil.prefixValue(prefix)
        }
    }
}

data class ConfigurationPropertyHolder(private val prefixByQualifiedName: Map<String, String>) {

    fun getPrefix(psiClass: PsiClass): String {
        return psiClass.qualifiedName?.let { prefixByQualifiedName[it] } ?: ""
    }
}

data class ConfigurationPropertyClassPrefix(val psiClass: PsiClass, val prefix: String)