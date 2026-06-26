/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringConfigurationPropertiesConstructorBindingInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringConfigurationPropertiesConstructorBindingInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringConfigurationPropertiesConstructorBindingInspection::class.java)
    }

    fun testRedundantConstructorBindingOnConstructor() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties <warning>@ConstructorBinding</warning> constructor(
                val first: String,
                val second: Long
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testRedundantConstructorBindingOnClass() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            <warning><error>@ConstructorBinding</error></warning>
            data class SomeProperties(
                val first: String,
                val second: Long
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testNoConstructorBinding() {
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                val first: String,
                val second: Long
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testMultipleConstructorsNoWarning() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties @ConstructorBinding constructor(
                val first: String,
                val second: Long
            ) {
                constructor() : this("", 0)
            }
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        myFixture.testHighlighting("SomeProperties.kt")
    }

    fun testRemoveRedundantConstructorBindingFromConstructorQuickFix() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties @ConstructorB<caret>inding constructor(
                val first: String,
                val second: Long
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        val fixName = SpringCoreBundle.message("explyt.spring.inspection.kotlin.constructor.binding.redundant.fix")
        val intention = myFixture.findSingleIntention(fixName)
        myFixture.launchAction(intention)

        @Language("kotlin") val expected = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                val first: String,
                val second: Long
            )
        """.trimIndent()
        myFixture.checkResult(expected)
    }

    fun testRemoveRedundantConstructorBindingFromClassQuickFix() {
        @Language("kotlin") val code = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            @ConstructorB<caret>inding
            data class SomeProperties(
                val first: String,
                val second: Long
            )
        """.trimIndent()
        myFixture.configureByText("SomeProperties.kt", code)
        val fixName = SpringCoreBundle.message("explyt.spring.inspection.kotlin.constructor.binding.redundant.fix")
        val intention = myFixture.findSingleIntention(fixName)
        myFixture.launchAction(intention)

        @Language("kotlin") val expected = """
            import org.springframework.boot.context.properties.bind.ConstructorBinding
            import org.springframework.stereotype.Component
            import org.springframework.boot.context.properties.ConfigurationProperties
            
            @Component
            @ConfigurationProperties(prefix = "some.prefix")
            data class SomeProperties(
                val first: String,
                val second: Long
            )
        """.trimIndent()
        myFixture.checkResult(expected)
    }
}
