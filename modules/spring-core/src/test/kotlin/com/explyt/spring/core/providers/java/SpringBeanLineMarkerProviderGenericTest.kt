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