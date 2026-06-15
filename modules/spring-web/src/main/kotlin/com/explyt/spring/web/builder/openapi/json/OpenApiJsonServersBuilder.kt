/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.iterateWithComma
import com.explyt.spring.web.builder.openapi.OpenApiServersBuilder

class OpenApiJsonServersBuilder(indent: String = "", builder: StringBuilder) : OpenApiServersBuilder(indent, builder),
    JsonValueGenerator {

    override fun addServerUrl(url: String): OpenApiServersBuilder {
        serverBuilders.add(
            OpenApiJsonServerBuilder(url, "$indent  ", builder)
        )
        return this
    }

    override fun build() {
        if (serverBuilders.isEmpty()) return

        builder.appendLine(",")
        builder.append("""$indent"servers": [""")

        builder.iterateWithComma(serverBuilders) { serverBuilder ->
            serverBuilder.build()
        }

        builder.append("\n$indent]")
    }

}