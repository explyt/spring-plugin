/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.json

import com.explyt.spring.web.builder.openapi.OpenApiServerBuilder

class OpenApiJsonServerBuilder(private val url: String, indent: String, builder: StringBuilder) :
    OpenApiServerBuilder(indent, builder) {

    override fun build() {
        addLinesWithIndent(
            """
                 {
                   "url": "$url"
                 }
                 """,
            indent
        )
    }

}
