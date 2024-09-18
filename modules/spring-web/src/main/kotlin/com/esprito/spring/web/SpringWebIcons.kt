package com.esprito.spring.web

import com.intellij.openapi.util.IconLoader

object SpringWebIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringWebIcons.javaClass)

    val OpenApi = load("com/esprito/spring/web/icons/openApi.svg")
}