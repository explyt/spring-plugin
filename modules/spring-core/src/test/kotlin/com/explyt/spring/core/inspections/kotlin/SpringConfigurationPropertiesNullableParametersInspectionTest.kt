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

import com.explyt.spring.core.inspections.SpringConfigurationPropertiesNullableParametersInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringConfigurationPropertiesNullableParametersInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringConfigurationPropertiesNullableParametersInspection::class.java)
    }

    fun testConfigurationPropertiesError() {
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                var first: String?,
                <error>var mustBeNullable: String</error>
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testNoConfigurationProperties() {
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            //@ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                var first: String?,
                var notAProblem: String
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testConfigurationPropertiesSuccess() {
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                var first: String?,
                var notAProblemA: String = "A",
                val notAProblemB: String = "B"
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testConstructorBinding() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties @ConstructorBinding constructor(
                var first: String,
                var second: Long
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testNotConstructorMethod() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                var first: String?,
                var notAProblemA: String = "A",
                val notAProblemB: String = "B"
            ) {
                fun getTest(orgId1: String) = "test" + orgId1
            }        

        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }
}
