/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.properties.FoldedPropertyValue
import com.intellij.openapi.module.Module

/**
 * A request mapping path with its `${key}` and `${key:default}` placeholders replaced, the way Spring MVC resolves
 * them against the environment before registering the mapping.
 *
 * A key is read from the module's configuration files, else its default is used, and a placeholder that has neither
 * stays as written: an endpoint whose path depends on an environment variable keeps showing where the value goes
 * instead of pretending to know it.
 */
object MappingPathPlaceholders {

    fun resolve(module: Module, path: String): String =
        if (PLACEHOLDER_START !in path) path
        else resolve(path) { key -> FoldedPropertyValue.resolve(module, key)?.value }

    fun resolve(path: String, valueOf: (String) -> String?): String =
        if (PLACEHOLDER_START !in path) path else resolve(path, valueOf, emptySet())

    private fun resolve(text: String, valueOf: (String) -> String?, resolving: Set<String>): String =
        PLACEHOLDER.replace(text) { match ->
            val key = match.groupValues[1].trim()
            val default = match.groups[2]?.value
            if (key in resolving || resolving.size >= MAX_DEPTH) return@replace match.value
            val value = valueOf(key) ?: default ?: return@replace match.value
            resolve(value, valueOf, resolving + key)
        }

    private const val PLACEHOLDER_START = "\${"
    private const val MAX_DEPTH = 8
    private val PLACEHOLDER = Regex("""\$\{([^:{}]+)(?::([^{}]*))?}""")
}
