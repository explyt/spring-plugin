/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.properties.contributors

enum class ContextCategory(private val propertyName: String) {
    ROOT("root"),
    HINTS("hints"),
    HINTS_VALUES("values"),
    HINTS_PROVIDERS("providers"),
    HINTS_PARAMETERS("parameters"),
    GROUPS("groups"),
    PROPERTIES("properties"),
    DEPRECATION("deprecation");

    companion object {
        fun forProperty(propertyName: String): ContextCategory? {
            for (contextCategory in entries.toTypedArray()) {
                if (contextCategory.propertyName == propertyName) {
                    return contextCategory
                }
            }
            return null
        }
    }
}

enum class TypeVariant {
    DEFAULT,
    ARRAY,
    OBJECT,
    TEXT
}

enum class ItemVariant(val item: String, val variantType: TypeVariant) {
    GROUPS("groups", TypeVariant.ARRAY),
    PROPERTIES("properties", TypeVariant.ARRAY),
    HINTS("hints", TypeVariant.ARRAY),

    NAME("name", TypeVariant.TEXT),
    TYPE("type", TypeVariant.TEXT),
    DESCRIPTION("description", TypeVariant.TEXT),
    SOURCE_TYPE("sourceType", TypeVariant.TEXT),
    SOURCE_METHOD("sourceMethod", TypeVariant.TEXT),
    DEFAULT_VALUE("defaultValue", TypeVariant.DEFAULT),
    DEPRECATED("deprecated", TypeVariant.DEFAULT),
    DEPRECATION("deprecation", TypeVariant.OBJECT),
    LEVEL("level", TypeVariant.TEXT),
    REASON("reason", TypeVariant.TEXT),
    REPLACEMENT("replacement", TypeVariant.TEXT),
    SINCE("since", TypeVariant.TEXT),
    VALUES("values", TypeVariant.ARRAY),
    PROVIDERS("providers", TypeVariant.ARRAY),
    VALUE("value", TypeVariant.DEFAULT),
    PARAMETERS("parameters", TypeVariant.OBJECT),
    TARGET("target", TypeVariant.TEXT),
    CONCRETE("concrete", TypeVariant.DEFAULT),
    GROUP("group", TypeVariant.DEFAULT);

    companion object {
        fun findByName(identifier: String): ItemVariant? {
            for (variant in entries) {
                if (variant.item == identifier) {
                    return variant
                }
            }
            return null
        }
    }
}