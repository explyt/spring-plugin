/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties.SPRING_JPA_PROPERTIES
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import java.util.Collections
import java.util.IdentityHashMap

private val SETTINGS_INDEX = Key.create<CachedValue<Map<String, List<PsiField>>>>("explyt.hibernate.settings.index")
private val SETTINGS_CLASSES = listOf(
    SpringCoreClasses.HIBERNATE_CFG_AVAILABLE_SETTING,
    SpringCoreClasses.HIBERNATE_JPA_BOOT_SETTINGS,
    SpringCoreClasses.HIBERNATE_JPA_AVAILABLE_SETTINGS
)

class HibernateSettingsMapKeyTargets : ConfigurationMapKeyTargets {
    override fun findTargets(module: Module, mapProperty: ConfigurationProperty, mapKey: String): List<PsiElement> {
        if (mapProperty.name != SPRING_JPA_PROPERTIES) return emptyList()
        return getSettingsIndex(module)[mapKey].orEmpty()
    }

    private fun getSettingsIndex(module: Module): Map<String, List<PsiField>> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(module, SETTINGS_INDEX, {
            val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
            val index = buildIndex(project, scope)
            CachedValueProvider.Result.create(
                index,
                PsiModificationTracker.MODIFICATION_COUNT,
                ProjectRootManager.getInstance(project)
            )
        }, false)
    }

    private fun buildIndex(project: Project, scope: GlobalSearchScope): Map<String, List<PsiField>> {
        val classes = SETTINGS_CLASSES.mapNotNull { JavaPsiFacade.getInstance(project).findClass(it, scope) }
        val fields = Collections.newSetFromMap(IdentityHashMap<PsiField, Boolean>())
        val index = mutableMapOf<String, MutableList<PsiField>>()
        for (psiClass in classes) {
            for (field in psiClass.allFields) {
                ProgressManager.checkCanceled()
                if (!fields.add(field) || !field.hasModifierProperty(PsiModifier.STATIC) ||
                    !field.hasModifierProperty(PsiModifier.FINAL) ||
                    field.type.canonicalText != CommonClassNames.JAVA_LANG_STRING
                ) continue
                val constantValue = field.computeConstantValue() as? String ?: continue
                index.getOrPut(constantValue) { mutableListOf() }.add(field)
            }
        }
        return index.mapValues { (_, values) -> values.toList() }
    }
}
