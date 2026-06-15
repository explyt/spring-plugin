/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.iterateWithComma
import com.explyt.spring.web.builder.openapi.OpenApiPathsBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention

class OpenApiJsonPathsBuilder(indent: String = "", builder: StringBuilder = StringBuilder()) :
    OpenApiPathsBuilder(indent, builder), JsonValueGenerator {

    override fun addEndpoint(endpoint: AddEndpointToOpenApiIntention.EndpointInfo): OpenApiJsonPathsBuilder {
        val pathBuilder = pathBuilderByPath.computeIfAbsent(endpoint.path) {
            OpenApiJsonPathBuilder(endpoint.path, "$indent  ", builder)
        }

        pathBuilder.addEndpoint(endpoint)
        return this
    }

    override fun build() {
        builder.appendLine(",")
        builder.append("""$indent"paths": {""")

        builder.iterateWithComma(pathBuilderByPath.values.toList()) { pathBuilder ->
            pathBuilder.build()
        }

        builder.append("\n$indent}")
    }

}
