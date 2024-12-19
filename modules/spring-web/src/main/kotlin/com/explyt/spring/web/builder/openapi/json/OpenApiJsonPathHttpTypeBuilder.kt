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
import com.explyt.spring.web.builder.openapi.OpenApiPathHttpTypeBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_STRING
import com.explyt.spring.web.util.SpringWebUtil.simpleTypesMap

class OpenApiJsonPathHttpTypeBuilder(
    private val endpoint: AddEndpointToOpenApiIntention.EndpointInfo,
    private val httpType: String,
    indent: String = "",
    builder: StringBuilder = StringBuilder()
) : OpenApiPathHttpTypeBuilder(indent, builder), JsonValueGenerator {

    override fun build() {
        addLinesWithIndent(
            """
            "${httpType.lowercase()}": {
              "tags": [
                "${endpoint.tag}"
              ],
              "operationId": "${endpoint.methodName}"
            """,
            indent
        )
        if (endpoint.description.isBlank()) {
            builder.appendLine(",")
            builder.append("""$indent  "description": "${endpoint.methodName}"""")
        } else {
            builder.appendLine(",")
            builder.append(
                """$indent  "description": "${endpoint.description.lines().joinToString(" ")}""""
            )
        }

        buildParameters()
        buildRequestBody()
        buildResponses()

        builder.append("\n$indent}")
    }

    private fun buildParameters() {
        if (endpoint.pathVariables.isEmpty() && endpoint.requestParameters.isEmpty()) return

        builder.appendLine(",")
        builder.append("""$indent  "parameters": [""")

        val parameters =
            endpoint.pathVariables.map { "path" to it } +
                    endpoint.requestParameters.map { "query" to it } +
                    endpoint.requestHeaders.map { "header" to it }

        builder.iterateWithComma(parameters) { (position, parameter) ->
            buildParameter(position, parameter)
        }

        builder.append("\n$indent  ]")
    }

    private fun buildRequestBody() {
        val requestBodyInfo = endpoint.requestBodyInfo ?: return

        builder.append(",")
        addLinesWithIndent(
            """
            "requestBody": {
              "description": "${requestBodyInfo.name}",
              "required": ${requestBodyInfo.isRequired},
              "content": {
            """,
            "$indent  "
        )
        builder.iterateWithComma(consumes()) { contentType ->
            OpenApiJsonTypeBuilder(endpoint.returnTypeFqn, contentType, "$indent      ", builder)
                .build()
        }

        addLinesWithIndent(
            """
              }
            }
            """,
            "$indent  "
        )
    }

    private fun buildParameter(position: String, pathVariable: SpringWebUtil.PathArgumentInfo) {
        addLinesWithIndent(
            """
                {
                  "name": "${pathVariable.name}",
                  "in": "$position",
                  "description": "${pathVariable.name}",
                  "required": ${pathVariable.isRequired},
                """,
            "$indent    "
        )

        if (!pathVariable.defaultValue.isNullOrBlank()) {
            builder.appendLine()
            builder.append("""$indent      "default": "${pathVariable.defaultValue}",""")
        }

        builder.appendLine()
        builder.append("""$indent      "schema": {""")

        val typeFqn = pathVariable.typeFqn
        val type = if (simpleTypesMap.containsKey(typeFqn)) {
            simpleTypesMap.getValue(typeFqn)
        } else {
            OPENAPI_STRING
        }

        builder.iterateWithComma(type.trimIndent().lines()) { line ->
            val (tag, value) = line.split(":")
            builder.appendLine()
            builder.append("""$indent        "${tag.trim()}": "${value.trim()}"""")
        }

        addLinesWithIndent(
            """
              }
            }
            """,
            "$indent    "
        )
    }

    private fun buildResponses() {
        builder.append(",")
        addLinesWithIndent(
            """
            "responses": {
              "200": {
                "description": "Ok",
                "content": {
            """,
            "$indent  "
        )
        builder.iterateWithComma(produces()) { contentType ->
            OpenApiJsonTypeBuilder(endpoint.returnTypeFqn, contentType, "$indent        ", builder)
                .build()
        }

        addLinesWithIndent(
            """
                }
              }
            }
            """,
            "$indent  "
        )
    }

    private fun produces(): List<String> {
        return contentTypes(endpoint.produces)
    }

    private fun consumes(): List<String> {
        return contentTypes(endpoint.consumes)
    }

}
