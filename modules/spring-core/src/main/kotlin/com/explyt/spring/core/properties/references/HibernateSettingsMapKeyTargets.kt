/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.references

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties.SPRING_JPA_PROPERTIES
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.project.DumbService
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.Callable

private val SETTINGS_INDEX = Key.create<CachedValue<Map<String, List<PsiField>>>>("explyt.hibernate.settings.index")
private val SETTINGS_CLASSES = listOf(
    SpringCoreClasses.HIBERNATE_CFG_AVAILABLE_SETTING,
    SpringCoreClasses.HIBERNATE_JPA_BOOT_SETTINGS,
    SpringCoreClasses.HIBERNATE_JPA_AVAILABLE_SETTINGS
)

class HibernateSettingsMapKeyTargets : ConfigurationMapKeyTargets {
    override fun findTargets(module: Module, mapProperty: ConfigurationProperty, mapKey: String): List<PsiElement> {
        val project = module.project
        if (mapProperty.name != SPRING_JPA_PROPERTIES || module.isDisposed || project.isDisposed ||
            DumbService.isDumb(project)
        ) return emptyList()
        val index = module.getUserData(SETTINGS_INDEX)?.upToDateOrNull?.get()
        if (index != null) return index[mapKey].orEmpty()

        ReadAction.nonBlocking(Callable { getSettingsIndex(module) })
            .inSmartMode(project)
            .expireWith(project)
            .expireWhen { module.isDisposed }
            .coalesceBy(SETTINGS_INDEX, module)
            .submit(AppExecutorUtil.getAppExecutorService())
        return emptyList()
    }

    private fun getSettingsIndex(module: Module): Map<String, List<PsiField>> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(module, SETTINGS_INDEX, {
            val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
            val classes = SETTINGS_CLASSES.mapNotNull { JavaPsiFacade.getInstance(project).findClass(it, scope) }
            val index = buildIndex(classes)
            CachedValueProvider.Result.create(
                index,
                ModificationTrackerManager.getInstance(project).getLibraryTracker(),
                ProjectRootManager.getInstance(project),
                *(classes.map { it.containingFile } + index.values.flatten().map { it.containingFile }).toTypedArray()
            )
        }, false)
    }

    private fun buildIndex(classes: List<PsiClass>): Map<String, List<PsiField>> {
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
