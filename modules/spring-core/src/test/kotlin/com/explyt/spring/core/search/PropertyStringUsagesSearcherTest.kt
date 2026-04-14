/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PropertyStringUsagesSearcherTest {
    private lateinit var propertyStringUsagesSearcher: PropertyStringUsagesSearcher

    @Before
    fun setUp() {
        propertyStringUsagesSearcher = PropertyStringUsagesSearcher()
    }

    @Test
    fun testSameText() {
        val result = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo.barBaz", "foo.barBaz")
        assertEquals(listOf("foo.bar-baz"), result)
    }

    @Test
    fun testOriginalNotEqualResolved() {
        val result = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo.barBaz", "foo.bar-baz")
        assertEquals(listOf("foo.bar-baz"), result)
    }

    @Test
    fun testSameTextWithDash() {
        val result = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo.bar-baz", "foo.bar-baz")
        assertEquals(listOf("foo.barbaz"), result)
    }

    @Test
    fun testOriginalNotEqualResolved2() {
        val result = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo.bar-baz", "foo.barBaz")
        assertEquals(listOf("foo.barbaz"), result)
    }

    @Test
    fun testComplex() {
        val result = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo.BarBazTest", "foo.barBazTest")
        assertEquals(listOf("foo.bar-baz-test"), result)
    }

    @Test
    fun testComplexDash() {
        val result = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo.BarBazTest", "foo.bar-Baz_Test")
        assertEquals(setOf("foo.bar-baz-test", "foo.bar-baz_test"), result.toSet())
    }

    @Test
    fun testSimple() {
        val result1 = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo", "foo")
        assertTrue(result1.isEmpty())

        val result2 = propertyStringUsagesSearcher.getAdditionalWordsToSearch("foo.barbaz", "foo.barbaz")
        assertTrue(result2.isEmpty())
    }
}