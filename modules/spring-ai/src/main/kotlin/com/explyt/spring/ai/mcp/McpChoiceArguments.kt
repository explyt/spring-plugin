/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.mcpserver.mcpFail

/**
 * Reads a string argument that names one of a fixed set of values.
 *
 * An unknown value is rejected with the valid ones listed, because filtering by it answers with nothing, which a caller
 * cannot tell from "nothing of that kind exists": an agent whose tool schema still advertised a dropped filter value
 * concluded that a project had no Actuator endpoints. The value is matched case-insensitively and trimmed, as the
 * tools always accepted it.
 */
internal object McpChoiceArguments {

    /** The value of [raw] among [allowed], or `null` when [raw] is blank and the argument may be left out. */
    fun optional(raw: String, argument: String, allowed: Collection<String>): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        return allowed.firstOrNull { it.equals(value, ignoreCase = true) }
            ?: mcpFail("Unknown $argument '$value'. Valid values: ${allowed.joinToString(", ")}.")
    }

    /** The value of [raw] among [allowed]; a blank value is rejected like an unknown one. */
    fun required(raw: String, argument: String, allowed: Collection<String>): String =
        optional(raw, argument, allowed)
            ?: mcpFail("$argument must not be empty. Valid values: ${allowed.joinToString(", ")}.")
}
