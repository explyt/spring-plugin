/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.web.builder.openapi.OpenApiComponentsBuilder

class OpenApiJsonComponentsBuilder(indent: String = "", builder: StringBuilder = StringBuilder()) :
    OpenApiComponentsBuilder(indent, builder), JsonValueGenerator {

    override fun build() {
        if (types.isEmpty()) return

        val schemasBuilder = OpenApiJsonComponentsSchemasBuilder("$indent  ", builder)
        for (type in types) {
            schemasBuilder.addType(type)
        }

        builder.appendLine()
        builder.append("""$indent"components": {""")

        schemasBuilder.build()

        builder.append("\n$indent}")
    }

}