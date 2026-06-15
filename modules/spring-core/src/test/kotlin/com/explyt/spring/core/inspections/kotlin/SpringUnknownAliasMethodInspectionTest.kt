/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringUnknownAliasMethodInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringUnknownAliasMethodInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringUnknownAliasMethodInspection::class.java)
    }

    fun testMethodsAliasFor() {
        @Language("kotlin")
        val testClass = """
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
                
                @get:AliasFor(
                    annotation = AncestorAnnotation::class,
                    attribute = <error descr="Cannot find attribute">NOT_EXISTING_METHOD</error>
                ) val unknownConstForAncestor: Array<String> = [],
                @get:AliasFor(
                    annotation = AncestorAnnotation::class,
                    attribute = <error descr="Cannot find attribute">"unknownForAncestor"</error>
                ) val unknownForAncestor: Array<String> = [],
                @get:AliasFor(
                    annotation = OtherAnnotation::class,
                    attribute = <error descr="Cannot find attribute">"value"</error>
                ) val notMetaAnnotated: Array<String> = []
            ) {
                companion object {
                    const val EXISTING_METHOD: String = "value"
                    const val NOT_EXISTING_METHOD: String = "notExistingMethod"    
                }
            }
        """.trimIndent()
        myFixture.configureByText("MethodAliasForConfiguration.kt", testClass)
        myFixture.testHighlighting("MethodAliasForConfiguration.kt")
    }
}