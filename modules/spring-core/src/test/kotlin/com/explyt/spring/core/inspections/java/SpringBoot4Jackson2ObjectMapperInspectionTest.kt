/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4Jackson2ObjectMapperInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4Jackson2ObjectMapperInspectionTest : ExplytInspectionJavaTestCase() {
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

    fun testAutowiredFieldReported() {
        myFixture.configureByText(
            "WebhookController.java",
            """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;
            
            @Component
            public class WebhookController {
                @Autowired
                <warning descr="No Jackson 2 ObjectMapper bean in Spring Boot 4: auto-configuration provides only Jackson 3 beans (tools.jackson.databind.json.JsonMapper)">ObjectMapper</warning> objectMapper;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("WebhookController.java")
    }

    fun testConstructorParameterReported() {
        myFixture.configureByText(
            "WebhookController.java",
            """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.stereotype.Component;
            
            @Component
            public class WebhookController {
                private final ObjectMapper objectMapper;
            
                public WebhookController(<warning descr="No Jackson 2 ObjectMapper bean in Spring Boot 4: auto-configuration provides only Jackson 3 beans (tools.jackson.databind.json.JsonMapper)">ObjectMapper</warning> objectMapper) {
                    this.objectMapper = objectMapper;
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("WebhookController.java")
    }

    fun testBeanMethodParameterReported() {
        myFixture.configureByText(
            "JacksonConfig.java",
            """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            
            @Configuration
            public class JacksonConfig {
                @Bean
                public String jsonInfo(<warning descr="No Jackson 2 ObjectMapper bean in Spring Boot 4: auto-configuration provides only Jackson 3 beans (tools.jackson.databind.json.JsonMapper)">ObjectMapper</warning> objectMapper) {
                    return objectMapper.toString();
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("JacksonConfig.java")
    }

    fun testJackson3JsonMapperNotReported() {
        myFixture.configureByText(
            "WebhookController.java",
            """
            import org.springframework.stereotype.Component;
            import tools.jackson.databind.json.JsonMapper;
            
            @Component
            public class WebhookController {
                private final JsonMapper jsonMapper;
            
                public WebhookController(JsonMapper jsonMapper) {
                    this.jsonMapper = jsonMapper;
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("WebhookController.java")
    }

    fun testLocalUsageNotReported() {
        myFixture.configureByText(
            "PlainService.java",
            """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.stereotype.Component;
            
            @Component
            public class PlainService {
                private final ObjectMapper objectMapper = new ObjectMapper();
            
                public String describe(ObjectMapper other) {
                    return other.toString();
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("PlainService.java")
    }

    fun testJackson2OptInNotReported() {
        myFixture.addClass(
            "package org.springframework.boot.jackson2.autoconfigure; public class Jackson2AutoConfiguration { }"
        )
        myFixture.configureByText(
            "WebhookController.java",
            """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;
            
            @Component
            public class WebhookController {
                @Autowired
                ObjectMapper objectMapper;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("WebhookController.java")
    }

    fun testQuickFixReplacesType() {
        myFixture.configureByText(
            "WebhookController.java",
            """
            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.stereotype.Component;
            
            @Component
            public class WebhookController {
                private final ObjectMapper objectMapper;
            
                public WebhookController(Object<caret>Mapper objectMapper) {
                    this.objectMapper = objectMapper;
                }
            }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("JsonMapper") }
        requireNotNull(intention) { "Replace-type fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue("parameter type should be replaced:\n$result", result.contains("WebhookController(JsonMapper objectMapper)"))
    }
}
