package com.explyt.jpa

import com.intellij.openapi.util.IconLoader

object JpaIcons {
    private fun load(path: String) = IconLoader.getIcon(path, JpaIcons.javaClass)

    val Alias = load("com/explyt/jpa/icons/alias.svg")
}