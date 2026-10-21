/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType

class ServerPortPlaceholderHttpCompletionTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
    )

    fun testNestedPlaceholderFallsBackToTheInnerDefault() {
        myFixture.addFileToProject(
            "application.properties",
            "server.port=\${SERVER_PORT:\${app.server.port:9317}}\n"
        )

        assertEquals(setOf("http://localhost:9317", "http://localhost:8080"), completedLocalServers())
    }

    fun testNestedPlaceholderTakesTheInnerKeyValue() {
        myFixture.addFileToProject(
            "application.properties",
            "server.port=\${SERVER_PORT:\${app.server.port:9317}}\napp.server.port=9400\n"
        )

        assertEquals(setOf("http://localhost:9400", "http://localhost:8080"), completedLocalServers())
    }

    fun testNestedPlaceholderInYaml() {
        myFixture.addFileToProject(
            "application.yml",
            "server:\n  port: \${SERVER_PORT:\${app.server.port:9317}}\n"
        )

        assertEquals(setOf("http://localhost:9317", "http://localhost:8080"), completedLocalServers())
    }

    fun testPlainPort() {
        myFixture.addFileToProject("application.properties", "server.port=8081\n")

        assertEquals(setOf("http://localhost:8081", "http://localhost:8080"), completedLocalServers())
    }

    fun testSingleLevelPlaceholderDefault() {
        myFixture.addFileToProject("application.properties", "server.port=\${server.port.custom:8082}\n")

        assertEquals(setOf("http://localhost:8082", "http://localhost:8080"), completedLocalServers())
    }

    fun testNoServerPortMeansTheDefaultPort() {
        assertEquals(setOf("http://localhost:8080"), completedLocalServers())
    }

    private fun completedLocalServers(): Set<String> {
        myFixture.configureByText("requests.http", "GET <caret>\n")
        myFixture.complete(CompletionType.BASIC)
        val lookupStrings = myFixture.lookupElementStrings ?: error("Completion inserted a single variant")
        return lookupStrings.filterTo(linkedSetOf()) { it.startsWith("http://localhost") }
    }
}
