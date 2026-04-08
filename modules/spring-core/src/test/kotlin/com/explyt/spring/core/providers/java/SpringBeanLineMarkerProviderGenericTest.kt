/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetString
import org.intellij.lang.annotations.Language

class SpringBeanLineMarkerProviderGenericTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    override fun setUp() {
        super.setUp()
        @Language("JAVA") val genericClass = """
            public class KafkaTemplate<K, V> {      
            }
        """.trimIndent()
        myFixture.addClass(genericClass)
    }

    fun testGeneric() {
        @Language("JAVA") val configClass = """            
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;           
                    
            @Configuration
            class ConfigClass {
                @Bean KafkaTemplate<?,?> kafkaTemplate() { 
                    return new KafkaTemplate<String, Object>(); 
                }
            }
        """.trimIndent()

        @Language("JAVA") val sampleClass = """
            import org.springframework.beans.factory.annotation.Autowired;            
            import org.springframework.stereotype.Component;
                    
            @Component
            class SampleClass {
                @Autowired KafkaTemplate<String, Object> template;
            }
        """.trimIndent()

        myFixture.addClass(configClass)
        myFixture.configureByText("SampleClass.java", sampleClass)

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "kafkaTemplate()" }
        }.size, 1)
    }

    fun testGenericNoMatch() {
        @Language("JAVA") val configClass = """            
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;           
                    
            @Configuration
            class ConfigClass {
                @Bean KafkaTemplate<Integer, Object> kafkaTemplate() { 
                    return new KafkaTemplate<Integer, Object>(); 
                }
            }
        """.trimIndent()

        @Language("JAVA") val sampleClass = """
            import org.springframework.beans.factory.annotation.Autowired;            
            import org.springframework.stereotype.Component;
                    
            @Component
            class SampleClass {
                @Autowired KafkaTemplate<String, Object> template;
            }
        """.trimIndent()

        myFixture.addClass(configClass)
        myFixture.configureByText("SampleClass.java", sampleClass)

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)
        assertTrue(gutterTargetString.isEmpty())
    }

    fun testGenericNoMatchValue() {
        @Language("JAVA") val configClass = """            
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;           
                    
            @Configuration
            class ConfigClass {
                @Bean KafkaTemplate<Integer, Integer> kafkaTemplate() { 
                    return new KafkaTemplate<Integer, Integer>(); 
                }
            }
        """.trimIndent()

        @Language("JAVA") val sampleClass = """
            import org.springframework.beans.factory.annotation.Autowired;            
            import org.springframework.stereotype.Component;
                    
            @Component
            class SampleClass {
                @Autowired KafkaTemplate<Integer, String> template;
            }
        """.trimIndent()

        myFixture.addClass(configClass)
        myFixture.configureByText("SampleClass.java", sampleClass)

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)
        assertTrue(gutterTargetString.isEmpty())
    }
}