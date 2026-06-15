/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.web.builder.AbstractBuilder

abstract class OpenApiPathHttpTypeBuilder(indent: String, builder: StringBuilder) : AbstractBuilder(indent, builder) {

    protected fun contentTypes(types: Collection<String>): List<String> {
        return if (types.isEmpty()) {
            listOf("application/json")
        } else {
            types.toList()
        }
    }

}
