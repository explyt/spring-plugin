/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiPathsBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention

class OpenApiYamlPathsBuilder(indent: String, builder: StringBuilder) :
    OpenApiPathsBuilder(indent, builder), YamlKeyValueGenerator {

    override fun addEndpoint(endpoint: AddEndpointToOpenApiIntention.EndpointInfo): OpenApiYamlPathsBuilder {
        val pathBuilder = pathBuilderByPath.computeIfAbsent(endpoint.path) {
            OpenApiYamlPathBuilder(endpoint.path, "$indent  ", builder)
        }

        pathBuilder.addEndpoint(endpoint)
        return this
    }

    override fun build() {
        builder.appendLine()
        builder.append("${indent}paths:")
        for (pathBuilder in pathBuilderByPath.values) {
            pathBuilder.build()
        }
    }

}
