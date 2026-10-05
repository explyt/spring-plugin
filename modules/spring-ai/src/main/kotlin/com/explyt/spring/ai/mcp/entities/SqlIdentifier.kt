/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.entities

class SqlIdentifier private constructor(val name: String, val quoted: Boolean) {

    val quotedOrNull: Boolean? get() = if (quoted) true else null

    companion object {
        private val DELIMITERS = mapOf('`' to '`', '"' to '"')

        fun declared(text: String): SqlIdentifier {
            val closing = text.firstOrNull()?.let { DELIMITERS[it] }
            val delimited = closing != null && text.length > 2 && text.last() == closing
            return if (delimited) SqlIdentifier(text.substring(1, text.length - 1), quoted = true)
            else SqlIdentifier(text, quoted = false)
        }
    }
}
