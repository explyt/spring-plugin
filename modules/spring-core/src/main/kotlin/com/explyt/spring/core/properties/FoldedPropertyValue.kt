/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.service.ProfilesService
import com.explyt.util.ExplytPsiUtil.isTestFiles
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
                .sortedWith(
                    compareBy(
                        { it.sourceRootPriority() },
                        { it.priority(profilesService) },
                        { it.property.sourceFile })
                )
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

    /**
     * A test source root is absent from the classpath the application runs with, so it is ranked below every
     * production file regardless of profile. It is ranked, not filtered: a key defined only under `src/test` still
     * folds, which is what issue #381 brought those files into scope for.
     *
     * Without this rank the two are usually indistinguishable — both are named `application.yaml` and both are
     * profile-less, tying every other comparator and leaving the winner to index iteration order.
     */
    private fun sourceRootPriority(): Int =
        if (isTestFiles(property.psiElement)) TEST_SOURCE else PRODUCTION_SOURCE

}

private const val PRODUCTION_SOURCE = 0
private const val TEST_SOURCE = 1

private const val ACTIVE_PROFILE = 0
private const val PROFILE_LESS = 1
private const val INACTIVE_PROFILE = 2
