/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.entities

import junit.framework.TestCase

class SqlIdentifierTest : TestCase() {

    fun testBackticksAndDoubleQuotesDelimitAnIdentifier() {
        assertEquals("Cluster" to true, SqlIdentifier.declared("`Cluster`").pair())
        assertEquals("Cluster" to true, SqlIdentifier.declared("\"Cluster\"").pair())
    }

    fun testAnUndelimitedNameIsKeptAsDeclared() {
        assertEquals("cluster" to false, SqlIdentifier.declared("cluster").pair())
        assertEquals(null, SqlIdentifier.declared("cluster").quotedOrNull)
        assertEquals(true, SqlIdentifier.declared("`cluster`").quotedOrNull)
    }

    fun testOnlyAMatchingPairOfDelimitersCounts() {
        assertEquals("`Cluster\"" to false, SqlIdentifier.declared("`Cluster\"").pair())
        assertEquals("\"Cluster" to false, SqlIdentifier.declared("\"Cluster").pair())
        assertEquals("Cluster`" to false, SqlIdentifier.declared("Cluster`").pair())
        assertEquals("\"" to false, SqlIdentifier.declared("\"").pair())
        assertEquals("\"\"" to false, SqlIdentifier.declared("\"\"").pair())
    }

    fun testInnerCharactersSurviveUnchanged() {
        assertEquals("my table" to true, SqlIdentifier.declared("\"my table\"").pair())
        assertEquals("a\"b" to true, SqlIdentifier.declared("`a\"b`").pair())
    }

    private fun SqlIdentifier.pair() = name to quoted
}
