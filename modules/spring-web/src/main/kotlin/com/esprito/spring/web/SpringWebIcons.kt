package com.esprito.spring.web

import com.intellij.openapi.util.IconLoader

object SpringWebIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringWebIcons.javaClass)

    val OpenApiJson = load("com/esprito/spring/web/icons/open_api_json.svg")
    val OpenApiYaml = load("com/esprito/spring/web/icons/open_api_yaml.svg")
    val Endpoints = load("com/esprito/spring/web/icons/endpointsSidebar.svg")
}