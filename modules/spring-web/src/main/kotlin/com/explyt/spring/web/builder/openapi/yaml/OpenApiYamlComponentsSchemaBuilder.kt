/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiComponentsSchemaBuilder
import com.explyt.spring.web.editor.openapi.OpenApiUtils.ComponentSchemaInfo

class OpenApiYamlComponentsSchemaBuilder(
    private val typeInfo: ComponentSchemaInfo,
    indent: String,
    builder: StringBuilder
) : OpenApiComponentsSchemaBuilder(indent, builder), YamlKeyValueGenerator {

    override fun build() {
        val typeName = typeInfo.typeQN.split('.').last()

        addLinesWithIndent(
            """
            $typeName:
              type: object
            """,
            indent
        )
    }

}
