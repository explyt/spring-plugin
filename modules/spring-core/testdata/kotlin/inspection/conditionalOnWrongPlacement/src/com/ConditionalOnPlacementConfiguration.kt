package com

import org.springframework.boot.autoconfigure.condition.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
internal annotation class ConditionalOnPropertyA

@ConditionalOnPropertyA
@ConditionalOnBean
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnMissingClass
@ConditionalOnProperty
@Configuration
open class DependsOnPropertyConfiguration {
    @ConditionalOnPropertyA
    @ConditionalOnBean
    @ConditionalOnClass
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass
    @ConditionalOnProperty
    @Bean
    open fun valid(): String {
        return ""
    }

    @ConditionalOnPropertyA
    @ConditionalOnBean
    @ConditionalOnClass
    @ConditionalOnMissingBean
    @ConditionalOnMissingClass
    @ConditionalOnProperty
    fun notABean(): String {
        return ""
    }
}

@ConditionalOnPropertyA
@ConditionalOnBean
@ConditionalOnClass
@ConditionalOnMissingBean
@ConditionalOnMissingClass
@ConditionalOnProperty
internal class NotAComponent