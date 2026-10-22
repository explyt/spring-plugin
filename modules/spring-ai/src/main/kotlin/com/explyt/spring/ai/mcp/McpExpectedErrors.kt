/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

private const val MCP_EXPECTED_ERROR_CLASS = "com.intellij.mcpserver.McpExpectedError"

internal fun mcpFail(message: String): Nothing = throw createMcpExpectedError(message)

private fun createMcpExpectedError(message: String): Throwable {
    return runCatching {
        Class.forName(MCP_EXPECTED_ERROR_CLASS)
            .getConstructor(String::class.java)
            .newInstance(message) as Throwable
    }.getOrElse { IllegalArgumentException(message) }
}
