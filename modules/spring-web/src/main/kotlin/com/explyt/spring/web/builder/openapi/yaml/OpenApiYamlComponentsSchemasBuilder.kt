/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiComponentsSchemaBuilder
import com.explyt.spring.web.builder.openapi.OpenApiComponentsSchemasBuilder
import com.explyt.spring.web.editor.openapi.OpenApiUtils.ComponentSchemaInfo

class OpenApiYamlComponentsSchemasBuilder(indent: String, builder: StringBuilder) :
    OpenApiComponentsSchemasBuilder(indent, builder),
    YamlKeyValueGenerator {
    private val schemaBuilders = mutableListOf<OpenApiComponentsSchemaBuilder>()

    override fun addType(typeInfo: ComponentSchemaInfo): OpenApiComponentsSchemasBuilder {
        schemaBuilders += OpenApiYamlComponentsSchemaBuilder(typeInfo, "$indent  ", builder)

        return this
    }

    override fun build() {
        builder.appendLine()
        builder.append("${indent}schemas:")
        for (schemaBuilder in schemaBuilders) {
            schemaBuilder.build()
        }
    }

}
