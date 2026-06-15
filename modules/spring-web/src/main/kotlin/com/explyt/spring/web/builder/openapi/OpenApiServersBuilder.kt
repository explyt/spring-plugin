/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder

abstract class OpenApiServersBuilder(indent: String, builder: StringBuilder) : AbstractBuilder(indent, builder) {
    protected val serverBuilders = mutableListOf<OpenApiServerBuilder>()

    abstract fun addServerUrl(url: String): OpenApiServersBuilder
}
