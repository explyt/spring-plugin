package com.esprito.spring.core

import com.intellij.openapi.util.IconLoader

object SpringIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringIcons.javaClass)

    val Spring = load("com/esprito/spring/core/icons/spring.svg")
    val SpringBean = load("com/esprito/spring/core/icons/springBean.svg")
    val SpringBeanDependencies = load("com/esprito/spring/core/icons/showAutowiredDependencies.svg")
    val EventListener = load("com/esprito/spring/core/icons/listener.svg")
    val EventPublisher = load("com/esprito/spring/core/icons/publisher.svg")
}