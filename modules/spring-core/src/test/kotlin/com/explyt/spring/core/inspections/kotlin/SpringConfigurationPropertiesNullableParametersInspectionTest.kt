/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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

    fun testConfigurationPropertiesMultipleConstructorsError() {
        // With several constructors and no @ConstructorBinding, Spring falls back to JavaBean (setter) binding,
        // so non-nullable properties without defaults are still reported even on Spring Boot 3+.
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                var first: String?,
                <error>var mustBeNullable: String</error>
            ) {
                constructor() : this(null, "")
            }
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testSpringBoot3SingleConstructorNoError() {
        // Since Spring Boot 3.0 a single-constructor class is bound through its constructor automatically,
        // so non-nullable properties are valid and must not be reported.
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                var first: String?,
                var second: String
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testSpringBoot3AutowiredConstructorError() {
        // @Autowired prevents the constructor from being used as a property-binding constructor, so the JavaBean
        // binding rules still apply even when Spring Boot 3+ is detected.
        @Language("kotlin") val code = """
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties @Autowired constructor(
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
                fun getTest(orgId1: String) = "test"
            }        

        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }
}
