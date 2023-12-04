package com.esprito.spring.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.LayeredIcon

object SpringIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringIcons.javaClass)

    val Spring = load("com/esprito/spring/core/icons/spring.svg")
    val SpringBean = load("com/esprito/spring/core/icons/springBean.svg")
    val PropertyKey = load("com/esprito/spring/core/icons/propertyKey.svg")
    val SpringBeanDependencies = load("com/esprito/spring/core/icons/showAutowiredDependencies.svg")
    val EventListener = load("com/esprito/spring/core/icons/listener.svg")
    val EventPublisher = load("com/esprito/spring/core/icons/publisher.svg")
    val SpringFactories = LayeredIcon(Spring, AllIcons.Actions.New)
}