/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
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