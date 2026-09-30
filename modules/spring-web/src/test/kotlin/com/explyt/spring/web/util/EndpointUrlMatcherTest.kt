/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.web.util.EndpointUrlMatcher.Policy
import com.explyt.spring.web.util.EndpointUrlMatcher.ReadingKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointUrlMatcherTest {

    private val routes = listOf("/api/items/{id}", "/api/items/export", "/api/items", "/{tenant}/reports")

    private fun search(url: String, basePath: String? = null) =
        EndpointUrlMatcher.match(routes, url, Policy.SEARCH, { it }, { basePath })

    private fun reference(url: String, basePath: String? = null) =
        EndpointUrlMatcher.match(routes, url, Policy.REFERENCE, { it }, { basePath })

    @Test
    fun `a concrete value in the URL matches the template of the route`() {
        val match = search("/api/items/42")

        assertEquals(listOf("/api/items/{id}"), match.endpoints)
        assertEquals(ReadingKind.AS_WRITTEN, match.reading?.kind)
    }

    @Test
    fun `the literal route Spring dispatches to comes before the template route`() {
        assertEquals(listOf("/api/items/export", "/api/items/{id}"), search("/api/items/export").endpoints)
    }

    @Test
    fun `a pasted URL is read without its scheme, host, query and fragment`() {
        assertEquals(listOf("/api/items/{id}"), search("https://example.com:8443/api/items/42?expand=true#top").endpoints)
    }

    @Test
    fun `a declared base path is stripped and reported as declared`() {
        val match = search("/t/api/items/42", basePath = "/t")

        assertEquals(listOf("/api/items/{id}"), match.endpoints)
        assertEquals(ReadingKind.DECLARED_BASE_PATH, match.reading?.kind)
        assertEquals("/t", match.reading?.prefix)
    }

    @Test
    fun `a base path must end at a segment boundary`() {
        assertTrue(reference("/tapi/items/42", basePath = "/t").endpoints.isEmpty())
    }

    @Test
    fun `only a route of the module declaring the base path answers under it`() {
        val served = mapOf("/api/items/{id}" to "/shop", "/api/orders/{id}" to null)
        val match = EndpointUrlMatcher.match(served.keys, "/shop/api/orders/7", Policy.REFERENCE, { it }, { served[it] })

        assertTrue("An application without /shop does not serve under it, got ${match.endpoints}", match.endpoints.isEmpty())
    }

    @Test
    fun `a search guesses an undeclared prefix and says it is a guess`() {
        val match = search("https://example.com/gateway/t/api/items/42")

        assertEquals(listOf("/api/items/{id}"), match.endpoints)
        assertEquals(ReadingKind.ASSUMED_PREFIX, match.reading?.kind)
        assertEquals("/gateway/t", match.reading?.prefix)
    }

    @Test
    fun `a reference never guesses a prefix`() {
        assertTrue(reference("/t/api/items/42").endpoints.isEmpty())
    }

    @Test
    fun `a guessed prefix never hands the rest of the URL to a route opening with a template`() {
        assertTrue(
            "Under /t the rest '/acme/reports' would fit /{tenant}/reports, which is evidence of nothing",
            search("/t/acme/reports").endpoints.isEmpty()
        )
        assertEquals(listOf("/{tenant}/reports"), search("/acme/reports").endpoints)
    }

    @Test
    fun `a URL matching as written wins over any base path`() {
        val match = reference("/api/items", basePath = "/api")

        assertEquals(listOf("/api/items"), match.endpoints)
        assertEquals(ReadingKind.AS_WRITTEN, match.reading?.kind)
    }

    @Test
    fun `a search finds a route from a fragment typed as written, after the routes the path matches whole`() {
        assertEquals(listOf("/api/items/export", "/api/items/{id}"), search("items/").endpoints.sorted())
        assertEquals(
            listOf("/api/items", "/api/items/export", "/api/items/{id}"),
            search("/api/items").endpoints
        )
    }

    @Test
    fun `a reference finds no route from a fragment`() {
        assertTrue(reference("items").endpoints.isEmpty())
    }

    @Test
    fun `a reference to this machine resolves, a reference to another host does not`() {
        assertEquals(listOf("/api/items/{id}"), reference("http://localhost:8080/api/items/42").endpoints)
        assertEquals(listOf("/api/items/{id}"), reference("http://127.0.0.1/api/items/42").endpoints)
        assertTrue(reference("https://payments.example.com/api/items/42").endpoints.isEmpty())
        assertNull(EndpointUrlMatcher.requestPathOf("https://payments.example.com/api/items", Policy.REFERENCE))
    }

    @Test
    fun `a search reads a URL of any host`() {
        assertEquals(listOf("/api/items/{id}"), search("https://payments.example.com/api/items/42").endpoints)
    }

    @Test
    fun `addresses answers for one route`() {
        assertTrue(EndpointUrlMatcher.addresses("/api/items/{id}", "http://localhost:8080/t/api/items/42", "/t"))
        assertFalse(EndpointUrlMatcher.addresses("/api/items/{id}", "http://localhost:8080/t/api/items/42", null))
    }
}
