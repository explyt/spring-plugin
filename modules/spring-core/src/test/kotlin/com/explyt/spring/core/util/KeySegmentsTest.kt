/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import org.junit.Assert
import org.junit.Test

/**
 * Bracket notation keeps a map key with dots atomic; the YAML writer joins it with a dot while the
 * `.properties` writer glues it to the prefix. Both forms must segment identically past the bracket.
 */
class KeySegmentsTest {

    @Test
    fun testPlainKey() {
        Assert.assertEquals(listOf("a", "b", "c"), PropertyUtil.keySegments("a.b.c"))
    }

    @Test
    fun testBracketedMapKeyKeepsDots() {
        Assert.assertEquals(
            listOf("a", "b", "[x.y]", "c"),
            PropertyUtil.keySegments("a.b.[x.y].c")
        )
    }

    @Test
    fun testAdjacentBracketForm() {
        Assert.assertEquals(
            listOf("a", "b[x.y]", "c"),
            PropertyUtil.keySegments("a.b[x.y].c")
        )
    }

    @Test
    fun testListIndexSegment() {
        Assert.assertEquals(
            listOf("a", "b[0]", "c"),
            PropertyUtil.keySegments("a.b[0].c")
        )
    }

    @Test
    fun testTrailingBracketedMapKey() {
        Assert.assertEquals(listOf("a", "b", "[x.y]"), PropertyUtil.keySegments("a.b.[x.y]"))
    }

    @Test
    fun testUnclosedBracketSwallowsTheRest() {
        Assert.assertEquals(listOf("a", "b", "[x.y"), PropertyUtil.keySegments("a.b.[x.y"))
    }
}
