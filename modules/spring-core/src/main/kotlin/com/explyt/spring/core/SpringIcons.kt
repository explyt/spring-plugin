package com.explyt.spring.core

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.LayeredIcon

object SpringIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringIcons.javaClass)

    val Spring = load("com/explyt/spring/core/icons/spring.svg")
    val SpringBean = load("com/explyt/spring/core/icons/springBean.svg")
    val springBeanInactive = load("com/explyt/spring/core/icons/springBeanInactive.svg")
    val SpringSetting = load("com/explyt/spring/core/icons/springSetting.svg")
    val Property = load("com/explyt/spring/core/icons/property.svg")
    val PropertyKey = load("com/explyt/spring/core/icons/propertyKey.svg")
    val SpringBeanDependencies = load("com/explyt/spring/core/icons/showAutowiredDependencies.svg")
    val EventListener = load("com/explyt/spring/core/icons/listener.svg")
    val EventPublisher = load("com/explyt/spring/core/icons/publisher.svg")
    val Hint = load("com/explyt/spring/core/icons/hint.svg")
    val ReadAccess = load("com/explyt/spring/core/icons/readAccess.svg")
    val SpringFactories = LayeredIcon.layeredIcon(arrayOf(Spring, AllIcons.Actions.New))
    val SpringExplorer = load("com/explyt/spring/core/icons/springBootExplorer.svg")
    val SpringBoot = load("com/explyt/spring/core/icons/spring-promo.svg")
    val SpringBootToolWindow = load("com/explyt/spring/core/icons/springToolWindow.svg")
}