package com.esprito.spring.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.LayeredIcon

object SpringIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringIcons.javaClass)

    val Spring = load("com/esprito/spring/core/icons/spring.svg")
    val SpringBean = load("com/esprito/spring/core/icons/springBean.svg")
    val springBeanInactive = load("com/esprito/spring/core/icons/springBeanInactive.svg")
    val SpringSetting = load("com/esprito/spring/core/icons/springSetting.svg")
    val Property = load("com/esprito/spring/core/icons/property.svg")
    val PropertyKey = load("com/esprito/spring/core/icons/propertyKey.svg")
    val SpringBeanDependencies = load("com/esprito/spring/core/icons/showAutowiredDependencies.svg")
    val EventListener = load("com/esprito/spring/core/icons/listener.svg")
    val EventPublisher = load("com/esprito/spring/core/icons/publisher.svg")
    val Hint = load("com/esprito/spring/core/icons/hint.svg")
    val ReadAccess = load("com/esprito/spring/core/icons/readAccess.svg")
    val SpringFactories = LayeredIcon.layeredIcon(arrayOf(Spring, AllIcons.Actions.New))
    val SpringExplorer = load("com/esprito/spring/core/icons/springBootExplorer.svg")
    val SpringBoot = load("com/esprito/spring/core/icons/spring-promo.svg")
    val SpringBootToolWindow = load("com/esprito/spring/core/icons/springToolWindow.svg")
}