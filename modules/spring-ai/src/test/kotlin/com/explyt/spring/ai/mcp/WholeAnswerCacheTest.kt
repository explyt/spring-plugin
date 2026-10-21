/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class WholeAnswerCacheTest : BasePlatformTestCase() {

    private val mapper = ObjectMapper()
    private var projections = 0

    fun testAWalkOverThreePagesProjectsEveryRecordOnce() {
        repeat(PAGES) { page -> answer(query() + ("offset" to (page * PAGE_SIZE).toString())) }

        assertEquals("$ROUTES records over $PAGES pages", ROUTES, projections)
    }

    fun testAnUnrelatedPsiChangeReprojectsTheSameDigest() {
        val first = answer(query())

        myFixture.addFileToProject("notes.txt", "unrelated")
        val second = answer(query())

        assertEquals("Precondition: the PSI change forces a fresh projection", 2 * ROUTES, projections)
        assertEquals(first.digest, second.digest)
    }

    fun testAChangedAnswerChangesTheDigest() {
        val first = answer(query())

        myFixture.addFileToProject("notes.txt", "unrelated")
        val second = answer(query(), routes = ROUTES + 1)

        assertFalse(first.digest == second.digest)
    }

    fun testAnotherQueryIsProjectedSeparately() {
        answer(query("a"))
        answer(query("b"))
        answer(query("a"))

        assertEquals(2 * ROUTES, projections)
    }

    fun testTheOldestOfFiveQueriesIsEvicted() {
        (1..5).forEach { answer(query("q$it")) }
        answer(query("q1"))

        assertEquals(6 * ROUTES, projections)
    }

    private fun query(filter: String = "web") = mapOf("tool" to "test", "controllerFilter" to filter)

    private fun answer(query: Map<String, String?>, routes: Int = ROUTES): WholeAnswer =
        WholeAnswerCache.getInstance(project).answerFor(query) {
            WholeAnswer.project(mapper.createObjectNode(), routes, { index ->
                projections++
                mapper.createObjectNode().put("fullPath", "/api/route/$index")
            })
        }

    private companion object {
        const val ROUTES = 60
        const val PAGES = 3
        const val PAGE_SIZE = 20
    }
}
