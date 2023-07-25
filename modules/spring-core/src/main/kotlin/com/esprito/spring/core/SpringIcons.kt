package com.esprito.spring.core

import com.intellij.openapi.util.IconLoader

object SpringIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringIcons.javaClass)

    val Spring = load("com/esprito/spring/core/icons/spring.svg")
}