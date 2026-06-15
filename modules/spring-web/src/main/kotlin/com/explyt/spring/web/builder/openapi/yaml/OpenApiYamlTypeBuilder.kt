/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
