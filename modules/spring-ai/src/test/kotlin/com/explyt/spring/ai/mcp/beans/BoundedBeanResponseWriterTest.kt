/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BoundedBeanResponseWriterTest {

    private val mapper = ObjectMapper()

    private fun content(size: Int): BeanResponseContent = BeanResponseContent(
        "LOOKUP",
        mapper.createObjectNode().put("source", "STATIC").put("precision", "MODULE_ESTIMATE"),
        if (size == 1) "SINGLE" else "MULTIPLE", "COMPLETE", 0,
        totalCount = size,
        candidateAt = { n ->
            mapper.createObjectNode()
                .put("id", "b-$n").put("name", "clock$n").put("type", "java.time.Clock")
                .put("kind", "BEAN_METHOD").set<ObjectNode>(
                    "declaration", mapper.createObjectNode()
                        .put("filePath", "src/" + "long-segment/".repeat(8) + "Clock$n.kt").put("line", n + 1)
                )
        }
    )

    @Test
    fun `final serialized pages fit and reach every candidate`() {
        val writer = BoundedBeanResponseWriter()
        val all = content(18)
        val seen = mutableListOf<String>()
        var offset = 0
        var expected: String? = null
        repeat(18) {
            val json = writer.write(all, "r-fixed", BeanPageRequest(offset, 5, 1800, expected))
            assertTrue(json.length <= 1800)
            val root = mapper.readTree(json)
            assertEquals("OK", root["status"].asText())
            assertTrue(mapper.writeValueAsString(listOf(root)).length <= 2000)
            assertEquals(18, root["totalCount"].asInt())
            seen += root["candidates"].map { it["id"].asText() }
            if (root["nextOffset"].isNull) {
                assertEquals(18, seen.size)
                assertEquals(18, seen.toSet().size)
                return
            }
            val next = root["nextOffset"].asInt()
            assertTrue(next > offset)
            offset = next
            expected = root["revision"].asText()
        }
        fail("Continuation did not terminate")
    }

    @Test
    fun `revision binds query but page size can change`() {
        val query = mapOf("application" to "com.explyt.demo.App", "beanName" to "clock")
        val first = BeanQueryRevision.compute("model-1", query)
        assertNotEquals(first, BeanQueryRevision.compute("model-1", query + ("beanName" to "other")))
        assertNotEquals(first, BeanQueryRevision.compute("model-2", query))
        assertEquals(first, BeanQueryRevision.compute("model-1", query.toSortedMap()))
    }

    @Test
    fun `a continuation without the previous revision is rejected`() {
        val json = BoundedBeanResponseWriter().write(content(9), "r-fixed", BeanPageRequest(5, 5, 1800, null))

        val root = mapper.readTree(json)
        assertEquals("ERROR", root["status"].asText())
        assertEquals("INVALID_ARGUMENT", root["error"]["code"].asText())
    }

    @Test
    fun `a stale revision is reported as a changed result at any offset`() {
        val writer = BoundedBeanResponseWriter()

        for (offset in listOf(0, 5)) {
            val json = writer.write(content(9), "r-new", BeanPageRequest(offset, 5, 1800, "r-old"))
            val root = mapper.readTree(json)
            assertEquals("ERROR", root["status"].asText())
            assertEquals("RESULT_CHANGED", root["error"]["code"].asText())
        }
    }

    @Test
    fun `a page is served only for the records it was asked for`() {
        var projected = 0
        val counted = content(18).let { it.copy(candidateAt = { n -> projected++; it.candidateAt(n) }) }

        BoundedBeanResponseWriter().write(counted, "r-fixed", BeanPageRequest(0, 1, 1800, null))

        assertTrue("Projected $projected records for a page of one", projected <= 2)
    }

    @Test
    fun `a rejected page projects nothing at all`() {
        var projected = 0
        val counted = content(18).let { it.copy(candidateAt = { n -> projected++; it.candidateAt(n) }) }
        val writer = BoundedBeanResponseWriter()

        writer.write(counted, "r-new", BeanPageRequest(0, 5, 1800, "r-old"))
        writer.write(counted, "r-fixed", BeanPageRequest(5, 5, 1800, null))

        assertEquals(0, projected)
    }

    @Test
    fun `an offset past the end is a terminal empty page`() {
        val json = BoundedBeanResponseWriter().write(content(3), "r-fixed", BeanPageRequest(3, 5, 1800, "r-fixed"))

        val root = mapper.readTree(json)
        assertEquals("OK", root["status"].asText())
        assertEquals(3, root["totalCount"].asInt())
        assertEquals(0, root["candidates"].size())
        assertTrue(root["nextOffset"].isNull)
        assertEquals(false, root["truncated"].asBoolean())
    }

    @Test
    fun `a candidate that cannot fit is reported, never truncated`() {
        val huge = BeanResponseContent(
            "LOOKUP", mapper.createObjectNode().put("source", "STATIC"), "SINGLE", "COMPLETE", 0,
            totalCount = 1,
            candidateAt = { mapper.createObjectNode().put("id", "b-0").put("type", "x".repeat(1200)) }
        )

        val json = BoundedBeanResponseWriter().write(huge, "r-fixed", BeanPageRequest(0, 5, 600, null))

        val root = mapper.readTree(json)
        assertEquals("ERROR", root["status"].asText())
        assertEquals("RESPONSE_TOO_LARGE", root["error"]["code"].asText())
        assertTrue("The oversized value must not be echoed", json.length <= 512)
    }

    @Test
    fun `an invalid budget is rejected within a fixed small response`() {
        val writer = BoundedBeanResponseWriter()

        for (page in listOf(BeanPageRequest(maxChars = 511), BeanPageRequest(maxChars = 16001), BeanPageRequest(limit = 0), BeanPageRequest(limit = 51))) {
            val json = writer.write(content(3), "r-fixed", page)
            val root = mapper.readTree(json)
            assertEquals("ERROR", root["status"].asText())
            assertEquals("INVALID_ARGUMENT", root["error"]["code"].asText())
            assertTrue(json.length <= 512)
        }
    }

    @Test
    fun `escaped characters are counted as the client receives them`() {
        val escaped = BeanResponseContent(
            "LOOKUP", mapper.createObjectNode().put("source", "STATIC"), "MULTIPLE", "COMPLETE", 0,
            totalCount = 12,
            candidateAt = { n ->
                mapper.createObjectNode().put("id", "b-$n")
                    .put("name", "\\\"кавычки\n\uD83D\uDE00".repeat(3))
                    .put("type", "java.time.Clock")
            }
        )
        val writer = BoundedBeanResponseWriter()

        var offset = 0
        var expected: String? = null
        var seen = 0
        repeat(12) {
            val json = writer.write(escaped, "r-fixed", BeanPageRequest(offset, 5, 1800, expected))
            assertTrue("Serialized ${json.length} chars", json.length <= 1800)
            val root = mapper.readTree(json)
            assertEquals("OK", root["status"].asText())
            seen += root["candidates"].size()
            if (root["nextOffset"].isNull) {
                assertEquals(12, seen)
                return
            }
            offset = root["nextOffset"].asInt()
            expected = root["revision"].asText()
        }
        fail("Continuation did not terminate")
    }
}
