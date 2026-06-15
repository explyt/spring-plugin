/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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