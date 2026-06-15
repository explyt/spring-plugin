/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention

abstract class OpenApiPathsBuilder(indent: String, builder: StringBuilder) : AbstractBuilder(indent, builder) {
    protected val pathBuilderByPath = mutableMapOf<String, OpenApiPathBuilder>()

    abstract fun addEndpoint(endpoint: AddEndpointToOpenApiIntention.EndpointInfo): OpenApiPathsBuilder
}
