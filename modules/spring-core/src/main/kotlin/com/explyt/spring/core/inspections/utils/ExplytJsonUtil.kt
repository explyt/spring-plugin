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