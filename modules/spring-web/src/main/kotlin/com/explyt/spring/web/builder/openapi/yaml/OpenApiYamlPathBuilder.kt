/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiPathBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo

class OpenApiYamlPathBuilder(
    private val path: String,
    indent: String = "",
    builder: StringBuilder = StringBuilder()
) : OpenApiPathBuilder(indent, builder), YamlKeyValueGenerator {

    override fun build() {
        builder.appendLine()
        builder.append("$indent${path}:")
        for (endpoint in endpointInfos) {
            for (httpType in endpoint.requestMethods) {
                OpenApiYamlPathHttpTypeBuilder(endpoint, httpType, "$indent  ", builder)
                    .build()
            }
        }
    }

    override fun addEndpoint(endpoint: EndpointInfo): OpenApiYamlPathBuilder {
        super.addEndpoint(endpoint)
        return this
    }

}
