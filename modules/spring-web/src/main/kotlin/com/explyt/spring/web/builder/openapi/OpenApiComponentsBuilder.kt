/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder
import com.explyt.spring.web.editor.openapi.OpenApiUtils.ComponentSchemaInfo

abstract class OpenApiComponentsBuilder(indent: String, builder: StringBuilder) : AbstractBuilder(indent, builder) {
    protected val types = mutableSetOf<ComponentSchemaInfo>()

    fun addType(typeInfo: ComponentSchemaInfo): OpenApiComponentsBuilder {
        types += typeInfo
        return this
    }

}
