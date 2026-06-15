/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.iterateWithComma
import com.explyt.spring.web.builder.openapi.OpenApiPathBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo

class OpenApiJsonPathBuilder(
    private val path: String,
    indent: String = "",
    builder: StringBuilder = StringBuilder()
) : OpenApiPathBuilder(indent, builder), JsonValueGenerator {

    override fun build() {
        builder.appendLine()
        builder.append("""$indent"${path}": {""")

        val endpointsWithMethod = mutableListOf<EndpointWithMethod>()

        for (endpoint in endpointInfos) {
            for (httpType in endpoint.requestMethods) {
                endpointsWithMethod.add(EndpointWithMethod(endpoint, httpType))
            }
        }

        builder.iterateWithComma(endpointsWithMethod) { (endpoint, httpType) ->
            OpenApiJsonPathHttpTypeBuilder(endpoint, httpType, "$indent  ", builder)
                .build()
        }

        builder.append("\n$indent}")
    }

    override fun addEndpoint(endpoint: EndpointInfo): OpenApiJsonPathBuilder {
        super.addEndpoint(endpoint)
        return this
    }

    private data class EndpointWithMethod(val endpoint: EndpointInfo, val httpType: String)

}
