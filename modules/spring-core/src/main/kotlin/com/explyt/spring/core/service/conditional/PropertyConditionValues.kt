/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.properties.FoldedPropertyValue
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.util.PropertyUtil
import com.intellij.openapi.module.Module

class PropertyConditionValues(private val module: Module) {
    private val propertiesByKey = DefinedConfigurationPropertiesSearch.getInstance(module.project)
        .getPropertiesCommonKeyMap(module)
    private val profilesService = ProfilesService.getInstance(module.project)

    fun valueOf(key: String): String? = resolvePlaceholder(rawValueOf(key))

    private fun rawValueOf(key: String): String? {
        val loaded = propertiesByKey[PropertyUtil.toCommonPropertyForm(key)].orEmpty().filter { isLoaded(it) }
        return FoldedPropertyValue.choose(module, loaded)?.value
    }

    private fun isLoaded(property: DefinedConfigurationProperty): Boolean =
        profileOf(property.sourceFile)?.let { profilesService.compute(it) } ?: true

    private fun resolvePlaceholder(value: String?): String? {
        val placeholder = value?.let { PLACEHOLDER.matchEntire(it.trim()) } ?: return value
        val name = placeholder.groupValues[1]
        val default = placeholder.groups[2]?.value
        return rawValueOf(name) ?: default ?: value
    }

    private fun profileOf(sourceFile: String): String? = DefinedConfigurationPropertiesSearch.fileMask
        .matchEntire(sourceFile)
        ?.groupValues
        ?.get(1)
        ?.removePrefix("-")
        ?.ifBlank { null }

    private companion object {
        val PLACEHOLDER = Regex("""\$\{([^:}]+)(?::([^}]*))?}""")
    }
}
