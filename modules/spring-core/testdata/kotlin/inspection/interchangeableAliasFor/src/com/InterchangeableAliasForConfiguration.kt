package com

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Configuration
open class InterchangeableAliasForConfiguration {

    @Bean(value=["iafc"], name=["iafc"])
    open fun someMethod(): InterchangeableAliasClass? = null

    @Bean(value=["iafc2"])
    open fun someMethod2(): InterchangeableAliasClass2? = null

    @BeanA(renamedValue=["iafc3"], name=["iaf3c"])
    open fun someMetho3d(): InterchangeableAliasClass3? = null
}

open class InterchangeableAliasClass
open class InterchangeableAliasClass2
open class InterchangeableAliasClass3

@Target(allowedTargets = [AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS])
@Retention(AnnotationRetention.RUNTIME)
@Bean
annotation class BeanA(
    @get:AliasFor(annotation = Bean::class, attribute = "value")
    val renamedValue: Array<String> = arrayOf(),

    @get:AliasFor(annotation = Bean::class, attribute = "name")
    val name: Array<String> = arrayOf()
)