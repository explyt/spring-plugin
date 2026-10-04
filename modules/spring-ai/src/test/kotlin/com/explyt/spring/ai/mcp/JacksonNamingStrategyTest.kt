/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import junit.framework.TestCase

/**
 * Expected names are Jackson's own: the examples from the `SnakeCaseStrategy` Javadoc of jackson-databind 2.21, and
 * the acronym cases computed by hand from `NamingStrategyImpls.SNAKE_CASE` and `translateLowerCaseWithSeparator`.
 */
class JacksonNamingStrategyTest : TestCase() {

    fun testSnakeCaseFollowsJacksonsDocumentedExamples() {
        val expected = mapOf(
            "userName" to "user_name",
            "UserName" to "user_name",
            "USER_NAME" to "user_name",
            "user_name" to "user_name",
            "user" to "user",
            "User" to "user",
            "USER" to "user",
            "_user" to "user",
            "_User" to "user",
            "__user" to "_user",
            "user__name" to "user__name",
            "theWWW" to "the_www",
            "routingEnabled" to "routing_enabled",
        )
        assertEquals(expected, expected.mapValues { (name, _) -> JacksonNamingStrategy.SNAKE_CASE.translate(name) })
    }

    fun testSnakeCaseKeepsALeadingRunOfCapitalsTogether() {
        assertEquals("urlvalue", JacksonNamingStrategy.SNAKE_CASE.translate("URLValue"))
        assertEquals("url_value", JacksonNamingStrategy.SNAKE_CASE.translate("urlValue"))
        assertEquals("URLVALUE", JacksonNamingStrategy.UPPER_SNAKE_CASE.translate("URLValue"))
        assertEquals("USER_NAME", JacksonNamingStrategy.UPPER_SNAKE_CASE.translate("userName"))
    }

    fun testSeparatorStrategiesStartANewWordAtTheLastCapitalOfARun() {
        assertEquals("url-value", JacksonNamingStrategy.KEBAB_CASE.translate("URLValue"))
        assertEquals("user-name", JacksonNamingStrategy.KEBAB_CASE.translate("userName"))
        assertEquals("user-name", JacksonNamingStrategy.KEBAB_CASE.translate("UserName"))
        assertEquals("the-www", JacksonNamingStrategy.KEBAB_CASE.translate("theWWW"))
        assertEquals("url.value", JacksonNamingStrategy.LOWER_DOT_CASE.translate("URLValue"))
        assertEquals("user.name", JacksonNamingStrategy.LOWER_DOT_CASE.translate("userName"))
    }

    fun testCaseOnlyStrategies() {
        assertEquals("userName", JacksonNamingStrategy.LOWER_CAMEL_CASE.translate("userName"))
        assertEquals("UserName", JacksonNamingStrategy.UPPER_CAMEL_CASE.translate("userName"))
        assertEquals("URL", JacksonNamingStrategy.UPPER_CAMEL_CASE.translate("URL"))
        assertEquals("username", JacksonNamingStrategy.LOWER_CASE.translate("userName"))
    }

    fun testStrategyIsNamedByConstantOrByStrategyClass() {
        assertEquals(JacksonNamingStrategy.SNAKE_CASE, JacksonNamingStrategy.named("SNAKE_CASE"))
        assertEquals(JacksonNamingStrategy.SNAKE_CASE, JacksonNamingStrategy.named(" PropertyNamingStrategies.SNAKE_CASE "))
        assertEquals(
            JacksonNamingStrategy.SNAKE_CASE,
            JacksonNamingStrategy.named("com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy")
        )
        assertEquals(
            JacksonNamingStrategy.KEBAB_CASE,
            JacksonNamingStrategy.named("com.fasterxml.jackson.databind.PropertyNamingStrategies\$KebabCaseStrategy")
        )
        assertEquals(
            JacksonNamingStrategy.SNAKE_CASE,
            JacksonNamingStrategy.named("PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES")
        )
        assertEquals(JacksonNamingStrategy.SNAKE_CASE, JacksonNamingStrategy.named("LowerCaseWithUnderscoresStrategy"))
        assertNull(JacksonNamingStrategy.named("com.example.shop.LegacyNamingStrategy"))
        assertNull(JacksonNamingStrategy.named("snake_case"))
    }
}
