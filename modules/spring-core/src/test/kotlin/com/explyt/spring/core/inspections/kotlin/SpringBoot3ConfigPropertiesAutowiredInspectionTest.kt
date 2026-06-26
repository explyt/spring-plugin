/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringBoot3ConfigPropertiesAutowiredInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringBoot3ConfigPropertiesAutowiredInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBoot3ConfigPropertiesAutowiredInspection::class.java)
    }

    fun testBeanDependencyWithoutAutowiredReported() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.stereotype.Service
            
            @Service
            class MyService
            
            @ConfigurationProperties(prefix = "app")
            class AppProperties(
                val name: String?,
                <warning>val service: MyService</warning>
            )
        """.trimIndent()
        myFixture.configureByText("AppProperties.kt", code)
        myFixture.testHighlighting("AppProperties.kt")
    }

    fun testBeanDependencyWithAutowiredNotReported() {
        @Language("kotlin") val code = """
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.stereotype.Service
            
            @Service
            class MyService
            
            @ConfigurationProperties(prefix = "app")
            class AppProperties @Autowired constructor(
                val name: String?,
                val service: MyService
            )
        """.trimIndent()
        myFixture.configureByText("AppProperties.kt", code)
        myFixture.testHighlighting("AppProperties.kt")
    }

    fun testPlainPropertiesNotReported() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @ConfigurationProperties(prefix = "app")
            class AppProperties(
                val name: String?,
                val count: Int?
            )
        """.trimIndent()
        myFixture.configureByText("AppProperties.kt", code)
        myFixture.testHighlighting("AppProperties.kt")
    }

    fun testMultipleConstructorsNotReported() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.stereotype.Service
            
            @Service
            class MyService
            
            @ConfigurationProperties(prefix = "app")
            class AppProperties(
                val name: String?,
                val service: MyService
            ) {
                constructor() : this(null, MyService())
            }
        """.trimIndent()
        myFixture.configureByText("AppProperties.kt", code)
        myFixture.testHighlighting("AppProperties.kt")
    }
}
