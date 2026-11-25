/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionMarkPathTemplateTest {

    @Test
    fun `simplifyUrl keeps a lazy quantifier inside a template`() {
        assertEquals("/files/{name:.+?}", SpringWebUtil.simplifyUrl("/files/{name:.+?}"))
    }

    @Test
    fun `simplifyUrl keeps a negative lookahead inside a template`() {
        assertEquals("/my/{vanityName:(?!admin).*}", SpringWebUtil.simplifyUrl("/my/{vanityName:(?!admin).*}"))
    }

    @Test
    fun `simplifyUrl strips the query after a template with a question mark`() {
        assertEquals("/files/{name:.+?}", SpringWebUtil.simplifyUrl("/files/{name:.+?}?a=1"))
    }

    @Test
    fun `simplifyUrl strips the query after a plain path`() {
        assertEquals("/files/report.pdf", SpringWebUtil.simplifyUrl("/files/report.pdf?a=1"))
    }

    @Test
    fun `lazy quantifier template matches a file name`() {
        assertMatches("/files/{name:.+?}", "/files/a.txt")
    }

    @Test
    fun `negative lookahead template matches a vanity name`() {
        assertMatches("/my/{vanityName:(?!admin).*}", "/my/john")
    }

    @Test
    fun `optional dash template matches an item id`() {
        assertMatches("/v{version:\\d+}/items/{id:[a-z]+-?\\d*}", "/v1/items/ab-12")
    }

    @Test
    fun `double wildcard before a lookahead template matches a page`() {
        assertMatches("/**/{path:^(?!api).*}", "/web/page")
    }

    @Test
    fun `plain template matches an id`() {
        assertMatches("/api/items/{id}", "/api/items/42")
    }

    private fun assertMatches(endpoint: String, path: String) {
        val matched = runCatching { SpringWebUtil.isEndpointMatches(endpoint, path) }
        assertNull("Matching $path against $endpoint threw", matched.exceptionOrNull())
        assertTrue("$endpoint must match $path", matched.getOrThrow())
    }
}
