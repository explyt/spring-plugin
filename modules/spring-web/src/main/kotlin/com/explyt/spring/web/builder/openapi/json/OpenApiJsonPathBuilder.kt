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

        builder.iterateWithComma(operations()) { (endpoint, httpType) ->
            OpenApiJsonPathHttpTypeBuilder(endpoint, httpType, "$indent  ", builder)
                .build()
        }

        builder.append("\n$indent}")
    }

    override fun addEndpoint(endpoint: EndpointInfo): OpenApiJsonPathBuilder {
        super.addEndpoint(endpoint)
        return this
    }


}
