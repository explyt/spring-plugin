package com

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Profile
internal annotation class ProfileA

@Profile
@ProfileA
@Configuration
open class DependsOnPropertyConfiguration {
    @Profile
    @ProfileA
    @Bean
    open fun valid(): String {
        return ""
    }

    @Profile
    @ProfileA
    open fun notABean(): String {
        return ""
    }
}

@Profile
@ProfileA
class NotAComponent