/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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