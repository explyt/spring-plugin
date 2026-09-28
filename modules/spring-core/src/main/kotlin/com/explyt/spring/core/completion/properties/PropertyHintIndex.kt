/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.SpringProperties.POSTFIX_VALUES

/**
 * A lookup index over the metadata hint catalogue, keyed by the exact hint name.
 *
 * The property inspection asked five separate questions of the shape
 * `hints.any { it.name == property.key && … }`, once per property in the file, so a file of *n* keys walked the
 * whole hint list 5*n* times — and always to the end, because most keys have no hint at all. The names are exact
 * comparisons, not relaxed ones, so a plain hash map by name reproduces them.
 */
class PropertyHintIndex private constructor(
    private val byName: Map<String, List<PropertyHint>>,
    private val firstOccurrence: Map<String, Int>,
    private val valuesHints: List<IndexedValue<Pair<String, PropertyHint>>>,
) {

    /** Every hint declared under [name], in catalogue order. Empty when nothing declares it. */
    fun hintsNamed(name: String): List<PropertyHint> = byName[name].orEmpty()

    /**
     * The hint governing [propertyKey]: one declared under the key itself, or a `<prefix>.values` hint whose map
     * prefix the key starts with - the first of those in catalogue order, as the linear scan it replaces returned.
     */
    fun hintFor(propertyKey: String): PropertyHint? {
        val exact = byName[propertyKey]?.firstOrNull()?.let { firstOccurrence.getValue(propertyKey) to it }
        val byPrefix = valuesHints.firstOrNull { propertyKey.startsWith(it.value.first) }
            ?.let { it.index to it.value.second }
        return listOfNotNull(exact, byPrefix).minByOrNull { it.first }?.second
    }

    /**
     * Mirrors `hints.filter { it.name in names }.distinctBy { it.name }`: the first hint declared under each
     * distinct name, ordered by where that name first appears in the catalogue. Order is preserved because the
     * resulting values are rendered into the "unresolved value" message.
     */
    fun firstPerName(vararg names: String): List<PropertyHint> {
        return names.distinct()
            .mapNotNull { name ->
                val first = byName[name]?.firstOrNull() ?: return@mapNotNull null
                firstOccurrence.getValue(name) to first
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    companion object {
        fun of(hints: List<PropertyHint>): PropertyHintIndex {
            val byName = HashMap<String, MutableList<PropertyHint>>()
            val firstOccurrence = HashMap<String, Int>()
            val valuesHints = mutableListOf<IndexedValue<Pair<String, PropertyHint>>>()
            hints.forEachIndexed { order, hint ->
                byName.getOrPut(hint.name) { mutableListOf() }.add(hint)
                firstOccurrence.putIfAbsent(hint.name, order)
                val valuesIndex = hint.name.lastIndexOf(POSTFIX_VALUES)
                if (valuesIndex != -1) valuesHints += IndexedValue(order, hint.name.substring(0, valuesIndex) to hint)
            }
            return PropertyHintIndex(byName, firstOccurrence, valuesHints)
        }
    }
}
