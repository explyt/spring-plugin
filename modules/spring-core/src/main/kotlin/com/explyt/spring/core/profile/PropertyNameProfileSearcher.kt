/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.profile

import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch.Companion.fileMask
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class PropertyNameProfileSearcher : ProfileSearcher {

    override fun searchActiveProfiles(module: Module): List<String> {
        val project = module.project
        val propertiesSearch = DefinedConfigurationPropertiesSearch.getInstance(module.project)

        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getProfilesFromProperties(module, propertiesSearch) { property ->
                    property.sourceFile.startsWith("application.")
                },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    override fun searchProfiles(module: Module): List<String> {
        val project = module.project
        val propertiesSearch = DefinedConfigurationPropertiesSearch.getInstance(module.project)

        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                getProfilesFromProperties(module, propertiesSearch)
                        + propertiesSearch.searchPropertyFiles().flatMap { getProfiles(it) },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun getProfiles(psiFile: PsiFile): List<String> {
        val filename = psiFile.name
        return listOfNotNull(
            fileMask.find(filename)
                ?.groups
                ?.get(1)
                ?.value
                ?.substring(1)
        )
    }

    private fun getProfilesFromProperties(
        module: Module,
        propertiesSearch: DefinedConfigurationPropertiesSearch,
        customFilter: (DefinedConfigurationProperty) -> Boolean = { true }
    ): List<String> {
        return propertiesSearch.findProperties(module, SPRING_PROFILES_ACTIVE)
            .asSequence()
            .filter { customFilter.invoke(it) }
            .mapNotNull { it.value }
            .flatMap { it.split(',') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

}