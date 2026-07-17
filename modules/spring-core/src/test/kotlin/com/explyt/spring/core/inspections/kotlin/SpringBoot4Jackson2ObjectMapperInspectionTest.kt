/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringBoot4Jackson2ObjectMapperInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringBoot4Jackson2ObjectMapperInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package com.fasterxml.jackson.databind; public class ObjectMapper { }")
        myFixture.addClass("package tools.jackson.databind.json; public class JsonMapper { }")
        myFixture.enableInspections(SpringBoot4Jackson2ObjectMapperInspection::class.java)
    }

    fun testConstructorParameterReported() {
        @Language("kotlin") val code = """
            import com.fasterxml.jackson.databind.ObjectMapper
            import org.springframework.stereotype.Component
            
            @Component
            class WebhookController(
                private val objectMapper: <warning descr="No Jackson 2 ObjectMapper bean in Spring Boot 4: auto-configuration provides only Jackson 3 beans (tools.jackson.databind.json.JsonMapper)">ObjectMapper</warning>
            )
        """.trimIndent()
        myFixture.configureByText("WebhookController.kt", code)
        myFixture.testHighlighting("WebhookController.kt")
    }

    fun testJackson3JsonMapperNotReported() {
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Component
            import tools.jackson.databind.json.JsonMapper
            
            @Component
            class WebhookController(
                private val jsonMapper: JsonMapper
            )
        """.trimIndent()
        myFixture.configureByText("WebhookController.kt", code)
        myFixture.testHighlighting("WebhookController.kt")
    }

    fun testLocalUsageNotReported() {
        @Language("kotlin") val code = """
            import com.fasterxml.jackson.databind.ObjectMapper
            import org.springframework.stereotype.Component
            
            @Component
            class PlainService {
                private val objectMapper: ObjectMapper = ObjectMapper()
            
                fun describe(other: ObjectMapper): String = other.toString()
            }
        """.trimIndent()
        myFixture.configureByText("PlainService.kt", code)
        myFixture.testHighlighting("PlainService.kt")
    }

    fun testQuickFixReplacesType() {
        @Language("kotlin") val code = """
            import com.fasterxml.jackson.databind.ObjectMapper
            import org.springframework.stereotype.Component
            
            @Component
            class WebhookController(
                private val objectMapper: Object<caret>Mapper
            )
        """.trimIndent()
        myFixture.configureByText("WebhookController.kt", code)
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("JsonMapper") }
        requireNotNull(intention) { "Replace-type fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue("parameter type should be replaced:\n$result", result.contains("objectMapper: JsonMapper"))
    }
}
