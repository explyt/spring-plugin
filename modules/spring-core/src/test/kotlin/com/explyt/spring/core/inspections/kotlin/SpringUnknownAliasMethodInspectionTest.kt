/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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