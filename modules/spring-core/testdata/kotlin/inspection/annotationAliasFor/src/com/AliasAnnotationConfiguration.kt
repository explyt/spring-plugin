package com

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Configuration
class AliasAnnotationConfiguration {

    @Bean
    @InheritorAnnotation
    fun someMethod(): MethodAliasClass = MethodAliasClass()
}

class MethodAliasClass

@Target(ElementType.METHOD, ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
annotation class AncestorAnnotation {
    @AliasFor("value")
    fun name(): Array<String> = arrayOf()

    @AliasFor("name")
    fun value(): Array<String> = arrayOf()
}

@Target(ElementType.METHOD, ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
annotation class OtherAnnotation {
    @AliasFor("value")
    fun name(): Array<String> = arrayOf()

    @AliasFor("name")
    fun value(): Array<String> = arrayOf()
}

@Target(ElementType.METHOD, ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AncestorAnnotation
annotation class InheritorAnnotation {
    @AliasFor(annotation = AncestorAnnotation::class, attribute = "value")
    fun valid(): Array<String> = arrayOf()

    @AliasFor(annotation = MethodAliasClass::class)
    fun notAnAnnotation(): Array<String> = arrayOf()

    @AliasFor(annotation = OtherAnnotation::class, attribute = "value")
    fun notMetaAnnotated(): Array<String> = arrayOf()
}