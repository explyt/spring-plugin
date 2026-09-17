/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EndpointPathPatternsTest {

    @Test
    fun `a literal route wins over a template route for the same URL`() {
        val sorted = listOf("/api/routes/{id}", "/api/routes/export").sortedWith(EndpointPathPatterns.SPECIFICITY)

        assertEquals(listOf("/api/routes/export", "/api/routes/{id}"), sorted)
    }

    @Test
    fun `a capture wins over a wildcard, and a catch-all sorts last`() {
        val sorted = listOf("/files/**", "/files/*", "/files/{name}").sortedWith(EndpointPathPatterns.SPECIFICITY)

        assertEquals(listOf("/files/{name}", "/files/*", "/files/**"), sorted)
    }

    @Test
    fun `among equally specific routes the longer one wins`() {
        val sorted = listOf("/api/routes/{id}", "/api/routes/{id}/history").sortedWith(EndpointPathPatterns.SPECIFICITY)

        assertEquals(listOf("/api/routes/{id}/history", "/api/routes/{id}"), sorted)
    }

    @Test
    fun `a template segment on either side matches any segment`() {
        assertEquals(3, EndpointPathPatterns.sharedLeadingSegments("/api/routes/{id}", "/api/routes/export/preview"))
        assertEquals(3, EndpointPathPatterns.sharedLeadingSegments("/api/routes/export/preview", "/api/routes/{id}"))
    }

    @Test
    fun `the shared prefix stops at the first differing literal`() {
        assertEquals(2, EndpointPathPatterns.sharedLeadingSegments("/api/routes/export", "/api/routes/other/history"))
        assertEquals(0, EndpointPathPatterns.sharedLeadingSegments("/nothing/here", "/api/routes"))
    }

    @Test
    fun `the prefix is rebuilt from the requested path`() {
        assertEquals("/api/routes/export", EndpointPathPatterns.prefixOf("/api/routes/export/preview", 3))
        assertEquals("/", EndpointPathPatterns.prefixOf("/api", 0))
    }
}
