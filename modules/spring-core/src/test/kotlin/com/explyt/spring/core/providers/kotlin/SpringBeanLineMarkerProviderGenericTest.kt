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

    fun testGenericInterface() {
        @Language("kotlin") val genericInterface = """
            open class TestGeneric<T> {
                var body: T? = null
            }
        """.trimIndent()

        @Language("kotlin") val genericImplClass = """
            open class TestGenericV1 : TestGeneric<String>()
        """.trimIndent()

        @Language("kotlin") val demoService = """
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            class DemoService {
                @Autowired
                lateinit var generic: TestGeneric<String>

                @Bean
                fun genericV1() = TestGenericV1()
            }
        """.trimIndent()

        myFixture.addFileToProject("TestGeneric.kt", genericInterface)
        myFixture.addFileToProject("TestGenericV1.kt", genericImplClass)
        myFixture.configureByText("DemoService.kt", demoService)

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(1, gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "genericV1()" }
        }.size)
    }

    fun testGenericInterfaceNoMatch() {
        @Language("kotlin") val genericInterface = """
            open class TestGeneric<T> {
                var body: T? = null
            }
        """.trimIndent()

        @Language("kotlin") val genericImplClass = """
            open class TestGenericV1 : TestGeneric<String>()
        """.trimIndent()

        @Language("kotlin") val demoService = """
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            class DemoService {
                @Autowired
                lateinit var generic: TestGeneric<Int>

                @Bean
                fun genericV1() = TestGenericV1()
            }
        """.trimIndent()

        myFixture.addFileToProject("TestGeneric.kt", genericInterface)
        myFixture.addFileToProject("TestGenericV1.kt", genericImplClass)
        myFixture.configureByText("DemoService.kt", demoService)

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertTrue(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "genericV1()" }
        }.isEmpty())
    }


    fun testGenericBeanWrapper() {
        @Language("kotlin") val genericInterface = """
            open class TestGeneric<T> {
                var body: T? = null
            }
        """.trimIndent()

        @Language("kotlin") val genericImplClass = """
            open class TestGenericV1 : TestGeneric<String>()
        """.trimIndent()

        @Language("kotlin") val demoService = """
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            class DemoService {
                @Autowired lateinit var generic: TestGeneric<String>
                @Autowired lateinit var genericList: List<TestGeneric<String>>
                @Autowired lateinit var genericProvider: org.springframework.beans.factory.ObjectProvider<TestGeneric<String>>

                @Bean
                fun genericV1() = TestGenericV1()
            }
        """.trimIndent()

        myFixture.addFileToProject("TestGeneric.kt", genericInterface)
        myFixture.addFileToProject("TestGenericV1.kt", genericImplClass)
        myFixture.configureByText("DemoService.kt", demoService)

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(3, gutterTargetString.size)
        assertEquals(setOf("genericV1()"), gutterTargetString.flatten().toSet())
    }

    fun testGenericComponentWrapper() {
        @Language("kotlin") val genericInterface = """
            open class TestGeneric<T> {
                var body: T? = null
            }
        """.trimIndent()

        @Language("kotlin") val genericImplClass = """
            @org.springframework.stereotype.Component
            open class TestGenericV1 : TestGeneric<String>()
        """.trimIndent()

        @Language("kotlin") val demoService = """
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            class DemoService {
                @Autowired lateinit var generic: TestGeneric<String>
                @Autowired lateinit var genericList: List<TestGeneric<String>>
                @Autowired lateinit var genericProvider: org.springframework.beans.factory.ObjectProvider<TestGeneric<String>>                
            }
        """.trimIndent()

        myFixture.addFileToProject("TestGeneric.kt", genericInterface)
        myFixture.addFileToProject("TestGenericV1.kt", genericImplClass)
        myFixture.configureByText("DemoService.kt", demoService)

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(3, gutterTargetString.size)
        assertEquals(setOf("TestGenericV1"), gutterTargetString.flatten().toSet())
    }
}