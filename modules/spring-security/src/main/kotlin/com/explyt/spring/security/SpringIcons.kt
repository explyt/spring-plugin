package com.explyt.spring.security

import com.intellij.openapi.util.IconLoader

object SpringIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringIcons.javaClass)

    val SpringBean = load("com/explyt/spring/security/icons/springBean.svg")
}