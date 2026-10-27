/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which project a tool call is answered from.
 *
 * The rule is checked over a list of candidates rather than over [com.intellij.openapi.project.ProjectManager]:
 * a light fixture opens exactly one project, so the multi-project half of the rule would otherwise be
 * unreachable and could only be asserted by claiming it works.
 */
class McpProjectResolverTest {

    @Test
    fun `a path naming no open project is refused even when a single one is open`() {
        val choice = choose("/nowhere/at/all", "/home/demo")

        assertTrue("Expected a refusal, got $choice", choice is McpProjectChoice.NotFound)
    }

    @Test
    fun `an absent path is answered by the single open project`() {
        assertEquals(resolved("/home/demo"), choose(null, "/home/demo"))
    }

    @Test
    fun `a blank path is treated as an absent one`() {
        assertEquals(resolved("/home/demo"), choose("", "/home/demo"))
        assertEquals(resolved("/home/demo"), choose("   ", "/home/demo"))
    }

    @Test
    fun `a path matches regardless of a trailing slash or a redundant segment`() {
        assertEquals(resolved("/home/demo"), choose("/home/demo/", "/home/demo"))
        assertEquals(resolved("/home/demo"), choose("/home/other/../demo", "/home/demo"))
        assertEquals(resolved("/home/demo/"), choose("/home/demo", "/home/demo/"))
    }

    @Test
    fun `an absent path with several projects open asks for one instead of picking`() {
        val choice = choose(null, "/home/demo", "/home/other")

        assertTrue("Expected a request to disambiguate, got $choice", choice is McpProjectChoice.Ambiguous)
        assertTrue(
            "The caller cannot retry without being told the paths: ${message(choice)}",
            message(choice).contains("/home/demo") && message(choice).contains("/home/other")
        )
    }

    @Test
    fun `a path picks its own project out of several`() {
        assertEquals(resolved("/home/other"), choose("/home/other", "/home/demo", "/home/other"))
    }

    @Test
    fun `a malformed path is refused rather than matched`() {
        val choice = choose("\u0000broken", "/home/demo")

        assertTrue("Expected a refusal, got $choice", choice is McpProjectChoice.NotFound)
    }

    @Test
    fun `nothing is resolved when no project is open`() {
        val choice = McpProjectResolver.choose<String?>("/home/demo", emptyList()) { it }

        assertTrue("Expected a refusal, got $choice", choice is McpProjectChoice.NotFound)
    }

    private fun resolved(basePath: String) = McpProjectChoice.Resolved(basePath)

    private fun message(choice: McpProjectChoice<*>): String = when (choice) {
        is McpProjectChoice.NotFound -> choice.message
        is McpProjectChoice.Ambiguous -> choice.message
        is McpProjectChoice.Resolved -> ""
    }

    private fun choose(projectPath: String?, vararg basePaths: String?): McpProjectChoice<String?> =
        McpProjectResolver.choose(projectPath, basePaths.toList()) { it }
}
