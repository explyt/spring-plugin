package com

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
@ConditionalOnProperty
internal annotation class ConditionalOnPropertyA(
    @get:AliasFor(
        annotation = ConditionalOnProperty::class,
        value = "value"
    ) vararg val value: String = [],
    @get:AliasFor(
        annotation = ConditionalOnProperty::class,
        value = "name"
    ) val nameEx: Array<String> = [],
    @get:AliasFor(
        annotation = ConditionalOnProperty::class,
        value = "prefix"
    ) val prefixEx: String = ""
)

@Configuration
open class DependsOnPropertyConfiguration {
    @ConditionalOnPropertyA("")
    @Bean
    open fun valid1(): String {
        return ""
    }

    @ConditionalOnPropertyA(nameEx = [""])
    @Bean
    open fun valid2(): String {
        return ""
    }

    @ConditionalOnProperty
    @Bean
    open fun valueIsNotSet1(): String {
        return ""
    }

    @ConditionalOnPropertyA(prefixEx = "")
    @Bean
    open fun valueIsNotSet2(): String {
        return ""
    }
}