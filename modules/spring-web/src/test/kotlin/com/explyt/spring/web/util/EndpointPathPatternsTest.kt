/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.web.util.EndpointPathPatterns.PathReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointPathPatternsTest {

    @Test
    fun `capture rest sorts after a single segment wildcard`() {
        val sorted = listOf("/files/{*path}", "/files/*").sortedWith(EndpointPathPatterns.SPECIFICITY)

        assertEquals(listOf("/files/*", "/files/{*path}"), sorted)
    }

    @Test
    fun `capture rest shares every remaining request segment`() {
        assertEquals(4, EndpointPathPatterns.sharedLeadingSegments("/actuator/health/{*path}", "/actuator/health/db/redis"))
        assertEquals(4, EndpointPathPatterns.sharedLeadingSegments("/actuator/health/db/redis", "/actuator/health/{*path}"))
    }

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

    @Test
    fun `a URL is reduced to the request path a route can declare`() {
        assertEquals(
            "/t/api/short-links/42/activity",
            EndpointPathPatterns.requestPathOf("https://example.com:8443/t/api/short-links/42/activity?window=24h#top")
        )
        assertEquals("/api/items", EndpointPathPatterns.requestPathOf("  http://localhost/api/items/  "))
        assertEquals("/api/items", EndpointPathPatterns.requestPathOf("/api/items?page=2"))
        assertEquals("/", EndpointPathPatterns.requestPathOf("https://example.com"))
        assertEquals("/items", EndpointPathPatterns.requestPathOf("items"))
    }

    @Test
    fun `a path is read as written first, then under ever longer prefixes`() {
        assertEquals(
            listOf(
                PathReading(null, "/t/api/items"),
                PathReading("/t", "/api/items"),
                PathReading("/t/api", "/items"),
            ),
            EndpointPathPatterns.readingsOf("/t/api/items").toList()
        )
        assertEquals(listOf(PathReading(null, "/items")), EndpointPathPatterns.readingsOf("/items").toList())
    }

    @Test
    fun `a path read as written admits every route`() {
        val asWritten = PathReading(null, "/api/items")

        assertTrue(asWritten.admits("/{tenant}/items"))
        assertTrue(asWritten.admits("/other"))
    }

    @Test
    fun `under an assumed prefix only a route opening with the same literal segment is admitted`() {
        val underPrefix = PathReading("/t", "/api/items")

        assertTrue(underPrefix.admits("/api/{id}"))
        assertFalse("A template would claim whatever the prefix left over", underPrefix.admits("/{tenant}/items"))
        assertFalse(underPrefix.admits("/other/items"))
        assertFalse(underPrefix.admits("/"))
    }
}
