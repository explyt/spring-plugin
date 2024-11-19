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

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.iterateWithComma
import com.explyt.spring.web.builder.openapi.OpenApiTypeBuilder
import com.explyt.spring.web.util.SpringWebUtil.arrayTypes
import com.explyt.spring.web.util.SpringWebUtil.simpleTypesMap

class OpenApiJsonTypeBuilder(
    private val typeCanonical: String,
    indent: String = "",
    builder: StringBuilder = StringBuilder()
) : OpenApiTypeBuilder(indent, builder), JsonValueGenerator {

    override fun build() {
        val typeSplit = typeCanonical.split("<", ">")
        if (typeSplit.size == 3 && typeSplit.last()
                .isBlank() && typeSplit.first() in arrayTypes
        ) {
            val itemTypeCanonical = typeSplit[1]

            if (simpleTypesMap.containsKey(itemTypeCanonical)) {
                addLinesWithIndent(
                    """
                    "schema": {
                      "type": "array",
                      "items": {
                    """,
                    indent
                )
                buildSimpleType(itemTypeCanonical, "$indent    ")
                addLinesWithIndent(
                    """
                      }
                    }
                    """,
                    indent
                )

                return
            } else {
                addLinesWithIndent(
                    """
                    "application/json": {
                      "schema": {
                        "type": "array",
                        "items": {
                          "$REF_KEY": "#/components/schemas/${itemTypeCanonical.split('.').last()}"    
                        }
                      }
                    }
                    """,
                    indent
                )

                return
            }
        }

        if (simpleTypesMap.containsKey(typeCanonical)) {
            builder.appendLine()
            builder.append("""$indent"schema": {""")
            buildSimpleType(typeCanonical, "$indent  ")
            builder.append("\n$indent}")
            return
        }

        val userType = typeCanonical.split(".").last()

        //schema is on user now (but is planning)
        addLinesWithIndent(
            """
            "application/json": {
              "schema": {
                "$REF_KEY": "#/components/schemas/$userType"
              }
            }
            """,
            indent
        )
    }

    private fun buildSimpleType(itemTypeCanonical: String, indent: String) {
        val typeInfo = simpleTypesMap[itemTypeCanonical] ?: return

        builder.iterateWithComma(typeInfo.trimIndent().lines()) { line ->
            val (tag, value) = line.split(":")
            builder.appendLine()
            builder.append("""$indent"${tag.trim()}": "${value.trim()}"""")
        }

    }

    companion object {
        private const val REF_KEY = "\$ref"
    }

}
