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

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetString
import org.intellij.lang.annotations.Language

class SpringBeanLineMarkerProviderGenericTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    override fun setUp() {
        super.setUp()
        @Language("kotlin") val genericClass = "class KafkaTemplate<K, V>".trimIndent()
        myFixture.addClass(genericClass)
    }

    fun testGeneric() {
        @Language("kotlin") val sampleClass = """                 
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
                    
            @Configuration
            class ConfigClass {
            
                @Bean
                fun kafkaTemplate(): KafkaTemplate<Int, Any> {
                    return KafkaTemplate<Int, Any>()
                }
            }
            
            @Component
            class SampleClass(
                private val template: KafkaTemplate<Int, Any>
            )
        """.trimIndent()

        myFixture.configureByText("SampleClass.kt", sampleClass)

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "kafkaTemplate()" }
        }.size, 1)
    }

    fun testGenericAny() {
        @Language("kotlin") val sampleClass = """                 
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
                    
            @Configuration
            class ConfigClass {
            
                @Bean
                fun kafkaTemplate(): KafkaTemplate<*, *> {
                    return KafkaTemplate<Int, Any>()
                }
            }
            
            @Component
            class SampleClass(
                private val template: KafkaTemplate<Int, Any>
            )
        """.trimIndent()

        myFixture.configureByText("SampleClass.kt", sampleClass)

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "kafkaTemplate()" }
        }.size, 1)
    }

    fun testGenericNoMatch() {
        @Language("kotlin") val sampleClass = """                 
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
                    
            @Configuration
            class ConfigClass {
            
                @Bean
                fun kafkaTemplate(): KafkaTemplate<String, Any> {
                    return KafkaTemplate<String, Any>()
                }
            }
            
            @Component
            class SampleClass(
                private val template: KafkaTemplate<Int, Any>
            )
        """.trimIndent()

        myFixture.configureByText("SampleClass.kt", sampleClass)

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)
        assertTrue(gutterTargetString.isEmpty())
    }

    fun testGenericNoMatchValue() {
        @Language("kotlin") val sampleClass = """                 
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
                    
            @Configuration
            class ConfigClass {
            
                @Bean
                fun kafkaTemplate(): KafkaTemplate<String, String> {
                    return KafkaTemplate<String, String>()
                }
            }
            
            @Component
            class SampleClass(
                private val template: KafkaTemplate<String, Int>
            )
        """.trimIndent()

        myFixture.configureByText("SampleClass.kt", sampleClass)

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)
        assertTrue(gutterTargetString.isEmpty())
    }
}