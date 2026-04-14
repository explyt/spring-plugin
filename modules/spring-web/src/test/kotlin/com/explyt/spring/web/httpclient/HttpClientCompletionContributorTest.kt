/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER
import com.intellij.codeInsight.completion.CompletionType
import org.intellij.lang.annotations.Language

class HttpClientCompletionContributorTest : ExplytJavaLightTestCase() {

    fun testEmptyFileCompletion() {
        myFixture.configureByText("requests.http", "<caret>")
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertTrue(lookupElementStrings.contains("GET"))
        assertTrue(lookupElementStrings.contains("POST"))
        assertTrue(lookupElementStrings.any { it.startsWith("http") })
    }

    fun testHttpMethodCompletion() {
        @Language("HTTP-EXPLYT") val request = """
            D<caret>
            """
        myFixture.configureByText("requests.http", request)
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertTrue(lookupElementStrings.contains("DELETE"))
        assertTrue(lookupElementStrings.none { it.startsWith("http") })
    }

    fun testHttpUrlCompletion() {
        @Language("HTTP-EXPLYT") val request = """
            GET <caret>
            """
        myFixture.configureByText("requests.http", request)
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertTrue(lookupElementStrings.any { it.startsWith("http") })
        assertTrue(lookupElementStrings.contains(DEFAULT_SERVER))
        assertFalse(lookupElementStrings.contains("GET"))
    }

    fun testHttpHeaderCompletion() {
        @Language("HTTP-EXPLYT") val request = """
            GET https://explyt.ai 
            <caret>
            """
        myFixture.configureByText("requests.http", request)
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertFalse(lookupElementStrings.any { it.startsWith("http") })
        assertFalse(lookupElementStrings.contains("GET"))
        assertTrue(lookupElementStrings.any { it.startsWith("Content") })
    }

    fun testHttpHeaderEncodingCompletion() {
        @Language("HTTP-EXPLYT") val request = """
            GET https://explyt.ai
            Content-Encoding: <caret>
            """
        myFixture.configureByText("requests.http", request)
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertFalse(lookupElementStrings.any { it.startsWith("http") })
        assertFalse(lookupElementStrings.contains("GET"))
        assertFalse(lookupElementStrings.contains("text/html"))
        assertFalse(lookupElementStrings.any { it.contains("utf", true) })
        assertTrue(lookupElementStrings.contains("gzip"))
    }

    fun testHttpHeaderCharsetCompletion() {
        @Language("HTTP-EXPLYT") val request = """
            GET https://explyt.ai
            Content-Encoding: gzip
            Accept-Charset: <caret>
            """
        myFixture.configureByText("requests.http", request)
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertFalse(lookupElementStrings.any { it.startsWith("http") })
        assertFalse(lookupElementStrings.contains("GET"))
        assertFalse(lookupElementStrings.contains("text/html"))
        assertFalse(lookupElementStrings.contains("gzip"))
        assertTrue(lookupElementStrings.any { it.contains("utf", true) })
    }

    fun testHttpHeaderContentTypeCompletion() {
        @Language("HTTP-EXPLYT") val request = """
            GET https://explyt.ai
            Content-Encoding: gzip
            Accept-Charset: utf-8
            Content-Type: <caret>
            """
        myFixture.configureByText("requests.http", request)
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertFalse(lookupElementStrings.any { it.startsWith("http") })
        assertFalse(lookupElementStrings.contains("GET"))
        assertFalse(lookupElementStrings.contains("gzip"))
        assertFalse(lookupElementStrings.any { it.contains("utf", true) })
        assertTrue(lookupElementStrings.contains("text/html"))
    }

    fun testHttpHeaderContentBodyCompletion() {
        @Language("HTTP-EXPLYT") val request = """
            GET https://explyt.ai
            Content-Encoding: gzip
            Accept-Charset: utf-8
            Content-Type: text/html
            
            <caret>
            """
        myFixture.configureByText("requests.http", request)
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        lookupElementStrings ?: return
        assertFalse(lookupElementStrings.any { it.startsWith("http") })
        assertFalse(lookupElementStrings.contains("GET"))
        assertFalse(lookupElementStrings.contains("gzip"))
        assertFalse(lookupElementStrings.any { it.contains("utf", true) })
        assertFalse(lookupElementStrings.contains("text/html"))
        assertTrue(lookupElementStrings.any { it.startsWith("< ") })
        assertTrue(lookupElementStrings.any { it.startsWith("{\"") })
    }
}