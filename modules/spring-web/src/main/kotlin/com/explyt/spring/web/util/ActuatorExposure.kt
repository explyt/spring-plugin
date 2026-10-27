/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.completion.properties.DefinedConfigurationProperty
import com.explyt.spring.core.properties.FoldedPropertyValue
import com.explyt.spring.web.loader.EndpointExposure
import com.intellij.openapi.module.Module
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import java.util.Locale

/**
 * Which Actuator endpoints Spring Boot publishes over HTTP, decided the way its `IncludeExcludeEndpointFilter` does for
 * `management.endpoints.web.exposure`:
 * - an id is exposed when it is included and not excluded, so an exclusion always wins;
 * - an empty `include` means Boot's default set, which is `health` alone;
 * - `*` matches every id;
 * - ids compare in `EndpointId` form: lower case, letters and digits only, so `heap-dump` is `heapdump`.
 *
 * JMX exposure is a separate property pair and plays no part: it decides nothing about a URL.
 */
object ActuatorExposure {

    const val INCLUDE_KEY = "management.endpoints.web.exposure.include"
    const val EXCLUDE_KEY = "management.endpoints.web.exposure.exclude"

    private const val MATCH_ALL = "*"
    private const val SEPARATOR = ','
    private val DEFAULT_INCLUDES = Patterns(listOf("health"))

    /** The rule [module]'s configuration states, read from [definitions] — every key the module defines, grouped by key. */
    fun of(module: Module, definitions: Map<String, List<DefinedConfigurationProperty>>): Rule {
        val valueOf = { key: String -> FoldedPropertyValue.choose(module, definitions[key].orEmpty())?.value }
        fun patternsOf(key: String) = Patterns(
            itemsOf(module, definitions, key).flatMap { MappingPathPlaceholders.resolve(it, valueOf).split(SEPARATOR) }
        )
        return Rule(include = patternsOf(INCLUDE_KEY), exclude = patternsOf(EXCLUDE_KEY))
    }

    /** The form Boot compares endpoint ids in: `EndpointId` keeps only letters and digits, in lower case. */
    fun normalize(id: String): String = id.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)

    class Rule(private val include: Patterns, private val exclude: Patterns) {

        fun exposureOf(id: String): EndpointExposure {
            val included = (if (include.isEmpty) DEFAULT_INCLUDES else include).matches(id)
            val excluded = exclude.matches(id)
            return when {
                excluded == true || included == false -> EndpointExposure.NOT_EXPOSED
                included == true && excluded == false -> EndpointExposure.EXPOSED
                else -> EndpointExposure.UNKNOWN
            }
        }
    }

    /**
     * The ids one property lists. An item still holding a `${` placeholder could be any id, so an id the readable
     * items do not name is neither matched nor ruled out.
     */
    class Patterns(items: List<String>) {
        private val readable: Set<String>
        private val unreadable: Boolean

        init {
            val (placeholders, ids) = items.map { it.trim() }.filter { it.isNotEmpty() }
                .partition { MappingPathPlaceholders.PLACEHOLDER_START in it }
            readable = ids.toSet()
            unreadable = placeholders.isNotEmpty()
        }

        private val matchesAll = MATCH_ALL in readable
        private val normalized = readable.mapTo(HashSet(), ::normalize)

        val isEmpty: Boolean get() = readable.isEmpty() && !unreadable

        /** `true` or `false` when the readable items decide it, `null` when only an unreadable item could. */
        fun matches(id: String): Boolean? = when {
            matchesAll || normalize(id) in normalized -> true
            unreadable -> null
            else -> false
        }
    }

    /**
     * The raw items of a list-valued key, in each shape Spring binds to a `Set<String>`: a YAML sequence, a
     * comma-separated scalar, or indexed keys `key[0]`, `key[1]`. A YAML sequence has no scalar
     * [DefinedConfigurationProperty.value], so its items are read from the PSI.
     */
    private fun itemsOf(
        module: Module,
        definitions: Map<String, List<DefinedConfigurationProperty>>,
        key: String
    ): List<String> {
        val chosen = FoldedPropertyValue.choose(module, definitions[key].orEmpty())
        if (chosen != null) {
            val sequence = (chosen.property.psiElement as? YAMLKeyValue)?.value as? YAMLSequence
            return sequence?.items?.mapNotNull { (it.value as? YAMLScalar)?.textValue }
                ?: listOfNotNull(chosen.value)
        }
        val indexed = Regex("""${Regex.escape(key)}\[(\d+)]""")
        return definitions.keys.asSequence()
            .mapNotNull { candidate -> indexed.matchEntire(candidate)?.let { it.groupValues[1].toInt() to candidate } }
            .sortedBy { it.first }
            .mapNotNull { (_, indexedKey) -> FoldedPropertyValue.choose(module, definitions[indexedKey].orEmpty())?.value }
            .toList()
    }
}
