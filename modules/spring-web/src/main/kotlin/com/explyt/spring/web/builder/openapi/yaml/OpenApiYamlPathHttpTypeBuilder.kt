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

import com.explyt.spring.web.builder.openapi.OpenApiPathHttpTypeBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.OPENAPI_STRING
import com.explyt.spring.web.util.SpringWebUtil.simpleTypesMap

class OpenApiYamlPathHttpTypeBuilder(
    private val endpoint: AddEndpointToOpenApiIntention.EndpointInfo,
    private val httpType: String,
    indent: String = "",
    builder: StringBuilder = StringBuilder()
) : OpenApiPathHttpTypeBuilder(indent, builder), YamlKeyValueGenerator {

    override fun build() {
        addLinesWithIndent(
            """
            ${httpType.lowercase()}:
              tags:
                - ${endpoint.tag}
              operationId: ${endpoint.methodName}
            """,
            indent
        )

        if (endpoint.description.isBlank()) {
            builder.appendLine()
            builder.append("$indent  description: ${endpoint.methodName}")
        } else {
            builder.appendLine()
            builder.append("$indent  description: >-")
            addLinesWithIndent(endpoint.description, "$indent    ")
        }

        buildParameters()
        buildRequestBody()
        buildResponses()
    }

    private fun buildParameters() {
        if (endpoint.pathVariables.isEmpty() && endpoint.requestParameters.isEmpty()) return

        builder.appendLine()
        builder.append("$indent  parameters:")
        for (pathVariable in endpoint.pathVariables) {
            buildParameter("path", pathVariable)
        }
        for (requestParameter in endpoint.requestParameters) {
            buildParameter("query", requestParameter)
        }
        for (requestHeader in endpoint.requestHeaders) {
            buildParameter("header", requestHeader)
        }
    }

    private fun buildRequestBody() {
        val requestBodyInfo = endpoint.requestBodyInfo ?: return

        addLinesWithIndent(
            """
            requestBody:
              description: ${requestBodyInfo.name}
              required: ${requestBodyInfo.isRequired}
              content:
            """,
            "$indent  "
        )
        for (contentType in consumes()) {
            OpenApiYamlTypeBuilder(requestBodyInfo.typeFqn, contentType, "$indent      ", builder)
                .build()
        }
    }

    private fun buildParameter(position: String, pathVariable: SpringWebUtil.PathArgumentInfo) {
        addLinesWithIndent(
            """
                - name: ${pathVariable.name}
                  in: $position
                  description: ${pathVariable.name}
                  required: ${pathVariable.isRequired}
                """,
            "$indent    "
        )

        if (!pathVariable.defaultValue.isNullOrBlank()) {
            builder.appendLine()
            builder.append("$indent      default: ${pathVariable.defaultValue}")
        }

        builder.appendLine()
        builder.append("$indent      schema:")

        val typeFqn = pathVariable.typeFqn
        val type = if (simpleTypesMap.containsKey(typeFqn)) {
            simpleTypesMap.getValue(typeFqn)
        } else {
            OPENAPI_STRING
        }

        addLinesWithIndent(type, "$indent        ")
    }

    private fun buildResponses() {
        addLinesWithIndent(
            """
            responses:
              '200':
                description: Ok
                content:
            """,
            "$indent  "
        )
        for (contentType in produces()) {
            OpenApiYamlTypeBuilder(endpoint.returnTypeFqn, contentType, "$indent        ", builder)
                .build()
        }
    }

    private fun produces(): List<String> {
        return contentTypes(endpoint.produces)
    }

    private fun consumes(): List<String> {
        return contentTypes(endpoint.consumes)
    }

}
