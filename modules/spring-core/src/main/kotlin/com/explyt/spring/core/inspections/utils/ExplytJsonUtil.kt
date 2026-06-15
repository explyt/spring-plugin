/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.utils

import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty

object ExplytJsonUtil {

    fun addPropertyToObject(
        property: JsonProperty,
        jsonObject: JsonObject,
        generator: JsonElementGenerator
    ): JsonProperty? {
        val hasProperty = jsonObject.propertyList.isNotEmpty()
        val bracket = jsonObject.lastChild
        if (hasProperty) {
            jsonObject.addBefore(generator.createComma(), bracket)
        }
        val added = jsonObject.addBefore(property, bracket)

        return added as? JsonProperty
    }

    fun <T> StringBuilder.iterateWithComma(elements: List<T>, action: (T) -> Unit) {
        elements.firstOrNull()?.let { action.invoke(it) }

        for (i in 1..<elements.size) {
            this.append(",")
            action.invoke(elements[i])
        }
    }


}