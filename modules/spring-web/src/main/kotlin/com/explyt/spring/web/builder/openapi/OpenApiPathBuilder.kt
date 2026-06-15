/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo

abstract class OpenApiPathBuilder(indent: String, builder: StringBuilder) : AbstractBuilder(indent, builder) {
    protected val endpointInfos = mutableSetOf<EndpointInfo>()

    open fun addEndpoint(endpoint: EndpointInfo): OpenApiPathBuilder {
        endpointInfos.add(endpoint)

        return this
    }

}
