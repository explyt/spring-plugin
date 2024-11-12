package com

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor

@Configuration
open class MethodAliasForConfiguration {
    @InheritorAnnotation
    @Bean
    open fun someMethod(): MethodAliasClass? {
        return null
    }
}

class MethodAliasClass

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
internal annotation class AncestorAnnotation(
    @get:AliasFor("value") val name: Array<String> = [],
    @get:AliasFor("name") vararg val value: String = []
)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
internal annotation class OtherAnnotation(
    @get:AliasFor("value") val name: Array<String> = [],
    @get:AliasFor("name") vararg val value: String = []
)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
@AncestorAnnotation
internal annotation class InheritorAnnotation(
    @get:AliasFor("unknown") val name: Array<String> = [],
    @get:AliasFor(
        annotation = AncestorAnnotation::class,
        attribute = "value"
    ) vararg val value: String = [],
    @get:AliasFor(
        annotation = AncestorAnnotation::class,
        attribute = EXISTING_METHOD
    ) val value2: Array<String> = [],
    @get:AliasFor(
        annotation = AncestorAnnotation::class,
        attribute = NOT_EXISTING_METHOD
    ) val unknownConstForAncestor: Array<String> = [],
    @get:AliasFor(
        annotation = AncestorAnnotation::class,
        attribute = "unknownForAncestor"
    ) val unknownForAncestor: Array<String> = [],
    @get:AliasFor(
        annotation = OtherAnnotation::class,
        attribute = "value"
    ) val notMetaAnnotated: Array<String> = []
) {
    companion object {
        const val EXISTING_METHOD: String = "value"
        const val NOT_EXISTING_METHOD: String = "notExistingMethod"
    }
}