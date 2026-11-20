/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PathTemplateLiteralMatchingTest {
    @Test
    fun `literal parentheses and plus do not repeat a capture over a long path`() {
        val template = "/p/({x:a{2}}/)+tail"
        val path = "/p/" + "a/".repeat(10_000) + "tail"
        assertFalse(SpringWebUtil.isEndpointMatches(template, path))
        assertTrue(SpringWebUtil.isEndpointMatches(template, "/p/(a/)+tail"))
    }

    @Test
    fun `private use characters in a template are literal`() {
        for (char in listOf('\uE000', '\uE001')) {
            val template = "/p/$char"
            assertTrue(SpringWebUtil.isEndpointMatches(template, template))
            assertFalse(SpringWebUtil.isEndpointMatches(template, "/p/abc"))
        }
    }

    @Test
    fun `a literal backslash does not escape the query delimiter`() {
        assertEquals("/p/\\", SpringWebUtil.simplifyUrl("/p/\\?abc"))
    }

    @Test
    fun `an opening bracket is literal beside a constrained variable`() {
        assertTrue(SpringWebUtil.isEndpointMatches("/p/[/{x:.+?}", "/p/[/abc"))
    }

    @Test
    fun `a dot matches only a dot`() {
        assertTrue(SpringWebUtil.isEndpointMatches("/v1.2/x", "/v1.2/x"))
        assertFalse(SpringWebUtil.isEndpointMatches("/v1.2/x", "/v1x2/x"))
    }

    @Test
    fun `a backslash before a variable is literal`() {
        assertTrue(SpringWebUtil.isEndpointMatches("/p/\\{x}", "/p/\\abc"))
        assertFalse(SpringWebUtil.isEndpointMatches("/p/\\{x}", "/p/abc"))
    }

    @Test
    fun `constraints containing nested or escaped braces are ignored`() {
        for (template in listOf("/p/{x:[a-z]{2}}", "/p/{x:\\{braced\\}}")) {
            assertTrue(SpringWebUtil.isEndpointMatches(template, "/p/ab"))
            assertFalse(SpringWebUtil.isEndpointMatches(template, "/p/a/b"))
        }
    }
}
