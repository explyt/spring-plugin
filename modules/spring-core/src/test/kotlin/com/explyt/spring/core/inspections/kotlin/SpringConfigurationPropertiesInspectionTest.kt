/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringConfigurationPropertiesInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

class SpringConfigurationPropertiesInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringConfigurationPropertiesInspection::class.java)
        myFixture.addClass(
            """
            import org.springframework.boot.context.properties.ConfigurationProperties;

            @ConfigurationProperties(prefix = "abc")
            public class ConfigProperties { }
        """.trimIndent()
        )
    }

    fun testCorrectOnClass() {
        myFixture.configureByText(
            "TestClass.kt",
            """
            @${SpringCoreClasses.CONFIGURATION_PROPERTIES}("prefix")            
            class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }

    fun testCorrectOnMethod() {
        myFixture.configureByText(
            "TestClass.kt",
            """                        
            class TestClass {
                @${SpringCoreClasses.CONFIGURATION_PROPERTIES}("prefix")
                fun method():String { return "1"}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }

    fun testEmptyOnClass() {
        myFixture.configureByText(
            "TestClass.kt",
            """
            <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}()</warning>            
            class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }

    fun testEmptyOnMethod() {
        myFixture.configureByText(
            "TestClass.kt",
            """                        
            class TestClass {
                <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}()</warning>
                fun method():String { return "1"}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }

    fun testKebabOnClass() {
        myFixture.configureByText(
            "TestClass.kt",
            """
            <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("KEBAB")</warning>            
            class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }

    fun testKebabOnMethod() {
        myFixture.configureByText(
            "TestClass.kt",
            """                        
            class TestClass {
                <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("kebab_kebab")</warning>
                fun method():String { return "1"}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }

    fun testDuplicateOnClass() {
        myFixture.configureByText(
            "TestClass.kt",
            """
            <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("abc")</warning>            
            class TestClass {}
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }

    fun testDuplicateOnMethod() {
        myFixture.configureByText(
            "TestClass.kt",
            """                        
            class TestClass {
                <warning>@${SpringCoreClasses.CONFIGURATION_PROPERTIES}("abc")</warning>
                fun method():String { return "1"}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("TestClass.kt")
    }
}
