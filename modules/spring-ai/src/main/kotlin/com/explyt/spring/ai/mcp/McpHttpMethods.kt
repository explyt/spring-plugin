/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

internal object McpHttpMethods {

    private const val ANY_METHOD = "ALL"

    fun listed(requestMethods: List<String>): List<String> = requestMethods.ifEmpty { listOf(ANY_METHOD) }

    fun routeVerb(requestMethods: List<String>): String = listed(requestMethods).joinToString("|")
}
