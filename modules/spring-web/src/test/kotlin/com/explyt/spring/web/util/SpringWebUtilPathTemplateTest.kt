/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.test.ExplytJavaLightTestCase

class SpringWebUtilPathTemplateTest : ExplytJavaLightTestCase() {

    fun testPathTemplatesWithDifferentVariableNamesAreEqual() {
        val pairs = listOf(
            "/items/{id}" to "/items/{itemId}",
            "/items/{id:[0-9]{3}}" to "/items/{x}",
            "/files/{p:.+/.+}" to "/files/{q}",
            "/a/{*rest}" to "/a/{x}",
            "/items/{id}/" to "/items/{x}",
        )

        pairs.forEach { (path, otherPath) ->
            assertTrue("Expected equivalent templates: $path and $otherPath", SpringWebUtil.isSamePathTemplate(path, otherPath))
            assertTrue("Expected symmetry: $otherPath and $path", SpringWebUtil.isSamePathTemplate(otherPath, path))
        }
    }

    fun testPathTemplatesWithDifferentLiteralsOrSegmentCountsAreNotEqual() {
        assertFalse(SpringWebUtil.isSamePathTemplate("/a/{a}-{b}", "/a/{x}"))
        assertFalse(SpringWebUtil.isSamePathTemplate("/Items/{id}", "/items/{id}"))
        assertFalse(SpringWebUtil.isSamePathTemplate("/items/{id}", "/items/{id}/x"))
    }

    fun testPathTemplateVariablesHandleNestedExpressionsAndWildcards() {
        assertEquals(
            listOf("id", "rest", "p"),
            SpringWebUtil.pathTemplateVariables("/a/{id:[0-9]{3}}/{*rest}/{p:.+/.+}")
        )
    }

    fun testPathTemplateVariablesIgnoreACompositeVariableSegment() {
        assertEquals(emptyList<String>(), SpringWebUtil.pathTemplateVariables("/a/{a}-{b}"))
    }
}
