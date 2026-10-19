/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.core.inspections.utils.ExplytJsonUtil.iterateWithComma
import com.explyt.spring.web.builder.openapi.OpenApiComponentsSchemaBuilder
import com.explyt.spring.web.builder.openapi.OpenApiComponentsSchemasBuilder
import com.explyt.spring.web.editor.openapi.OpenApiUtils.ComponentSchemaInfo

class OpenApiJsonComponentsSchemasBuilder(indent: String, builder: StringBuilder) :
    OpenApiComponentsSchemasBuilder(indent, builder), JsonValueGenerator {
    private val schemaBuilders = mutableListOf<OpenApiComponentsSchemaBuilder>()
    private val typeQnSet = mutableSetOf<String>()

    override fun addType(typeInfo: ComponentSchemaInfo): OpenApiJsonComponentsSchemasBuilder {
        if (typeQnSet.add(typeInfo.typeQN)) {
            schemaBuilders += OpenApiJsonComponentsSchemaBuilder(typeInfo, "$indent  ", builder)
        }

        return this
    }

    override fun build() {
        builder.appendLine()
        builder.append("""$indent"schemas": {""")

        builder.iterateWithComma(schemaBuilders) { schemaBuilder ->
            schemaBuilder.build()
        }

        builder.append("\n$indent}")
    }

}
