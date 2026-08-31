/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.service.ProfilesService
import com.intellij.openapi.module.Module

/**
 * A configuration value chosen for folding out of every source that defines the key, together with the profile the
 * winning source belongs to.
 */
class FoldedPropertyValue(
    val property: DefinedConfigurationProperty,
    private val profile: String?
) {

    val value: String? get() = property.value

    val presentation: String? get() = value?.let { decorate(it) }

    /**
     * A value taken from `application-<profile>` is not the value the application runs with unless that profile is
     * active, so the origin is part of the folded text instead of being silently dropped.
     */
    fun decorate(text: String): String = profile
        ?.let { SpringCoreBundle.message("explyt.spring.folding.property.profile", text, it) }
        ?: text

    companion object {

        fun resolve(module: Module, key: String): FoldedPropertyValue? {
            val properties = DefinedConfigurationPropertiesSearch.getInstance(module.project)
                .findProperties(module, key)
            if (properties.isEmpty()) return null

            val profilesService = ProfilesService.getInstance(module.project)
            return properties.asSequence()
                .map { FoldedPropertyValue(it, profileOf(it.sourceFile)) }
                .sortedWith(compareBy({ it.priority(profilesService) }, { it.property.sourceFile }))
                .firstOrNull()
        }

        private fun profileOf(sourceFile: String): String? = DefinedConfigurationPropertiesSearch.fileMask
            .matchEntire(sourceFile)
            ?.groupValues
            ?.get(1)
            ?.removePrefix("-")
            ?.ifBlank { null }
    }

    /**
     * Spring Boot lets an active profile override the profile-less file, and never reads a file of an inactive one.
     */
    private fun priority(profilesService: ProfilesService): Int = when {
        profile == null -> PROFILE_LESS
        profilesService.compute(profile) -> ACTIVE_PROFILE
        else -> INACTIVE_PROFILE
    }

}

private const val ACTIVE_PROFILE = 0
private const val PROFILE_LESS = 1
private const val INACTIVE_PROFILE = 2
