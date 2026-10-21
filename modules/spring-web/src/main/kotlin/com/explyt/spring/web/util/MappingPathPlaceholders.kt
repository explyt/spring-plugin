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

    private fun resolve(text: String, valueOf: (String) -> String?, resolving: Set<String>): String {
        val start = text.indexOf(PLACEHOLDER_START)
        if (start < 0) return text
        val end = closingBraceOf(text, start + PLACEHOLDER_START.length) ?: return text
        val placeholder = text.substring(start, end + 1)
        val replacement = replacementOf(placeholder.removeSurrounding(PLACEHOLDER_START, "}"), valueOf, resolving)
        return text.substring(0, start) + (replacement ?: placeholder) +
                resolve(text.substring(end + 1), valueOf, resolving)
    }

    private fun replacementOf(placeholder: String, valueOf: (String) -> String?, resolving: Set<String>): String? {
        val separator = topLevelSeparatorOf(placeholder)
        val key = (if (separator < 0) placeholder else placeholder.substring(0, separator)).trim()
        if (key.isEmpty() || key.any { it == '{' || it == '}' }) return null
        if (key in resolving || resolving.size >= MAX_DEPTH) return null
        valueOf(key)?.let { return resolve(it, valueOf, resolving + key) }
        if (separator < 0) return null
        return resolve(placeholder.substring(separator + 1), valueOf, resolving)
            .takeUnless { PLACEHOLDER_START in it }
    }

    private fun closingBraceOf(text: String, from: Int): Int? {
        var depth = 0
        for (index in from until text.length) when (text[index]) {
            '{' -> depth++
            '}' -> if (depth == 0) return index else depth--
        }
        return null
    }

    private fun topLevelSeparatorOf(placeholder: String): Int {
        var depth = 0
        placeholder.forEachIndexed { index, char ->
            when (char) {
                '{' -> depth++
                '}' -> depth--
                ':' -> if (depth == 0) return index
            }
        }
        return -1
    }

    const val PLACEHOLDER_START = "\${"
    private const val MAX_DEPTH = 8
}
