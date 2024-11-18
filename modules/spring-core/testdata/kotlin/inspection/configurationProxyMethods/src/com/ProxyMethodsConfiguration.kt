package com

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Component
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

class ProxyBeanA

class ProxyBeanAA(val proxyBeanA: ProxyBeanA)

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Configuration
annotation class ConfigurationWithAlias(
    @AliasFor(annotation = Configuration::class)
    val proxyBeanMethods: Boolean = true
)

@ConfigurationWithAlias(proxyBeanMethods = false)
class ProxyMethodsConfiguration {
    @Bean
    fun proxyBeanA() = ProxyBeanA()

    @Bean
    fun proxyBeanAAIncorrect() = ProxyBeanAA(proxyBeanA()) // -> incorrect call

    @Bean
    fun proxyBeanAACorrect(proxyBeanA: ProxyBeanA) = ProxyBeanAA(proxyBeanA) // -> correct injected instance
}

@Component
class ProxyMethodsComponent {
    @Bean
    fun proxyBeanAFromComponent() = ProxyBeanA()

    @Bean
    fun proxyBeanAA() = ProxyBeanAA(proxyBeanAFromComponent()) // -> incorrect call
}