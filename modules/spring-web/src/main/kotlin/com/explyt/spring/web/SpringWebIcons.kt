/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web

import com.intellij.openapi.util.IconLoader

object SpringWebIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringWebIcons.javaClass)

    val OpenApiJson = load("com/explyt/spring/web/icons/open_api_json.svg")
    val OpenApiYaml = load("com/explyt/spring/web/icons/open_api_yaml.svg")
    val Endpoints = load("com/explyt/spring/web/icons/endpointsSidebar.svg")
    val HttpConnect = load("com/explyt/spring/web/icons/connect.svg")
    val HttpDelete = load("com/explyt/spring/web/icons/delete.svg")
    val HttpGet = load("com/explyt/spring/web/icons/get.svg")
    val HttpHead = load("com/explyt/spring/web/icons/head.svg")
    val HttpOptions = load("com/explyt/spring/web/icons/options.svg")
    val HttpPatch = load("com/explyt/spring/web/icons/patch.svg")
    val HttpPost = load("com/explyt/spring/web/icons/post.svg")
    val HttpPut = load("com/explyt/spring/web/icons/put.svg")
    val HttpTrace = load("com/explyt/spring/web/icons/trace.svg")
}