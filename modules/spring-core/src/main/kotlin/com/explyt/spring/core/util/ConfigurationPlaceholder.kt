/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.core.properties.providers.ValueConfigurationPropertyReferenceProvider
import com.intellij.openapi.util.TextRange

data class ConfigurationPlaceholder(
    val name: String,
    val defaultValue: String?,
    val range: TextRange,
    val redundantRanges: List<TextRange>,
) {
    val literalDefault: String?
        get() = defaultValue?.takeUnless { it.contains("\${") || it.contains("#{") }

    companion object {
        fun parse(value: String): ConfigurationPlaceholder? {
            val matcher = ValueConfigurationPropertyReferenceProvider.PROPERTIES_PATTERN.matcher(value)
            if (!matcher.matches()) return null
            val default = matcher.group(2)?.removePrefix(":")
            val range = TextRange(0, value.length)
            val redundantRanges = if (default == null) listOf(range) else listOf(
                TextRange(0, matcher.start(2) + 1),
                TextRange(value.lastIndex, value.length),
            )
            return ConfigurationPlaceholder(matcher.group(1), default, range, redundantRanges)
        }
    }
}
