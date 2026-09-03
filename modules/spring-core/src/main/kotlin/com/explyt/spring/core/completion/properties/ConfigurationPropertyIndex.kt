/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.util.PropertyUtil

/**
 * A lookup index over the configuration property catalogue, replacing a linear scan that re-normalised every
 * catalogue name on every query.
 *
 * The scan it replaces is `properties.find { PropertyUtil.isSameProperty(it.name, query, it.type) }`, and
 * `isSameProperty` normalises **both** sides with [PropertyUtil.toBooleanAlias] using the *catalogue entry's*
 * type. The type is only known once an entry is in hand, so one map keyed by the plain normal form cannot
 * express it. Hence two maps:
 *
 * - [plain] holds every non-boolean entry under `toCommonPropertyForm(name)`, since the alias is the identity
 *   for a non-boolean type;
 * - [booleanAliased] holds every boolean entry under `toCommonPropertyForm(toBooleanAlias(name, type))`, and a
 *   query is aliased the same way before it is looked up there.
 *
 * A query therefore costs two hash lookups instead of a full scan. When both maps answer, the entry that came
 * first in the original list wins, which is what `find` returned.
 */
class ConfigurationPropertyIndex private constructor(
    private val plain: Map<String, Entry>,
    private val booleanAliased: Map<String, Entry>,
) {

    private class Entry(val order: Int, val property: ConfigurationProperty)

    fun findProperty(propertyName: String): ConfigurationProperty? {
        val plainHit = plain[PropertyUtil.toCommonPropertyForm(propertyName)]
        val booleanHit = booleanAliased[
            PropertyUtil.toCommonPropertyForm(PropertyUtil.toBooleanAlias(propertyName, JavaCoreClasses.BOOLEAN))
        ]
        return when {
            booleanHit == null -> plainHit?.property
            plainHit == null -> booleanHit.property
            booleanHit.order < plainHit.order -> booleanHit.property
            else -> plainHit.property
        }
    }

    companion object {
        fun of(properties: List<ConfigurationProperty>): ConfigurationPropertyIndex {
            val plain = HashMap<String, Entry>()
            val booleanAliased = HashMap<String, Entry>()
            properties.forEachIndexed { order, property ->
                val key = PropertyUtil.toCommonPropertyForm(
                    PropertyUtil.toBooleanAlias(property.name, property.type)
                )
                // The catalogue can repeat a name across loaders; `find` returned the first, so keep the first.
                val target = if (property.isBooleanType()) booleanAliased else plain
                target.putIfAbsent(key, Entry(order, property))
            }
            return ConfigurationPropertyIndex(plain, booleanAliased)
        }
    }
}
