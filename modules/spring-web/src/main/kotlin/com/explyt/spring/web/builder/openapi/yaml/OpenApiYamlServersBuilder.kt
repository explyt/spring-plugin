/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiServersBuilder

class OpenApiYamlServersBuilder(indent: String = "", builder: StringBuilder) : OpenApiServersBuilder(indent, builder),
    YamlKeyValueGenerator {

    override fun addServerUrl(url: String): OpenApiServersBuilder {
        serverBuilders.add(
            OpenApiYamlServerBuilder(url, "$indent  ", builder)
        )
        return this
    }

    override fun build() {
        if (serverBuilders.isEmpty()) return

        builder.appendLine()
        builder.append("${indent}servers:")
        for (serverBuilder in serverBuilders) {
            serverBuilder.build()
        }
    }

}
