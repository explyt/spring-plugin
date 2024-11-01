package com.explyt.spring.aop

import com.intellij.openapi.util.IconLoader

object SpringAopIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringAopIcons.javaClass)

    val Advice = load("com/explyt/spring/aop/icons/abstractAdvice.svg")
}