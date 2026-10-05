/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.entities

class SqlIdentifier private constructor(val name: String, val quoted: Boolean) {

    val quotedOrNull: Boolean? get() = if (quoted) true else null

    companion object {
        private val DELIMITERS = mapOf('`' to '`', '"' to '"', '[' to ']')

        fun declared(text: String): SqlIdentifier {
            val trimmed = text.trim()
            val closing = trimmed.firstOrNull()?.let { DELIMITERS[it] }
            val delimited = closing != null && trimmed.length > 2 && trimmed.last() == closing
            return if (delimited) SqlIdentifier(trimmed.substring(1, trimmed.length - 1), quoted = true)
            else SqlIdentifier(trimmed, quoted = false)
        }
    }
}
