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

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiTypeBuilder
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.util.SpringWebUtil.MULTIPART_FILE
import com.explyt.spring.web.util.SpringWebUtil.simpleTypesMap

class OpenApiYamlTypeBuilder(
    private val typeCanonical: String,
    private val contentType: String,
    indent: String = "",
    builder: StringBuilder = StringBuilder()
) : OpenApiTypeBuilder(indent, builder), YamlKeyValueGenerator {

    override fun build() {
        val (typeQN, isCollection) = OpenApiUtils.unwrapType(typeCanonical)

        if (isCollection) {
            if (simpleTypesMap.containsKey(typeQN)) {
                addLinesWithIndent(
                    """
                    schema:
                      type: array
                      items:
                    """,
                    indent
                )
                buildSimpleType(typeQN, "$indent    ")
                return
            } else {
                addLinesWithIndent(
                    """
                    $contentType:
                      schema:
                        type: array
                        items:
                          $REF_KEY: '#/components/schemas/${typeQN.split('.').last()}'    
                    """,
                    indent
                )
                return
            }
        }

        if (typeQN == MULTIPART_FILE) {
            addLinesWithIndent(
                """
                    $contentType:
                      schema:
                        type: object
                        properties:
                          file:
                            type: string
                            format: binary
                    """,
                indent
            )
            return
        }

        if (simpleTypesMap.containsKey(typeQN)) {
            builder.appendLine()
            builder.appendLine("${indent}$contentType:")
            builder.append("$indent  schema:")
            buildSimpleType(typeQN, "$indent    ")
            return
        }

        val userType = typeQN.split(".").last()

        //schema is on user now (but is planning)
        addLinesWithIndent(
            """
            $contentType:
              schema:
                $REF_KEY: '#/components/schemas/$userType'    
            """,
            indent
        )
    }

    private fun buildSimpleType(itemTypeCanonical: String, indent: String) {
        val typeInfo = simpleTypesMap[itemTypeCanonical] ?: return
        addLinesWithIndent(typeInfo, indent)
    }

    companion object {
        private const val REF_KEY = "\$ref"
    }

}
