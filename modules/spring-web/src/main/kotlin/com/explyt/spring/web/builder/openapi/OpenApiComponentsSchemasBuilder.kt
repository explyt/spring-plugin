/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder
import com.explyt.spring.web.editor.openapi.OpenApiUtils

abstract class OpenApiComponentsSchemasBuilder(indent: String, builder: StringBuilder) :
    AbstractBuilder(indent, builder) {

    abstract fun addType(typeInfo: OpenApiUtils.ComponentSchemaInfo): OpenApiComponentsSchemasBuilder

}
