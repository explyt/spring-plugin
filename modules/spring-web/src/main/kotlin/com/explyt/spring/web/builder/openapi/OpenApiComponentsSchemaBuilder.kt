/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder

abstract class OpenApiComponentsSchemaBuilder(
    indent: String,
    builder: StringBuilder
) :
    AbstractBuilder(indent, builder) {
    protected val types = mutableSetOf<String>()

}
