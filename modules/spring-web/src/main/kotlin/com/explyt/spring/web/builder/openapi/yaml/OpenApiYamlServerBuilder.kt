/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi.yaml

import com.explyt.spring.web.builder.openapi.OpenApiServerBuilder

class OpenApiYamlServerBuilder(private val url: String, indent: String, builder: StringBuilder) :
    OpenApiServerBuilder(indent, builder) {

    override fun build() {
        builder.appendLine()
        builder.append("${indent}- url: $url")
    }

}
