/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringRecommendedEnumValueInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The recommendation on real Spring metadata rather than a fixture: `management.endpoint.<id>.access` is an `Access`
 * enum whose own default value ships as `unrestricted`, so `READ_ONLY` is reported and rewritten to `read-only`.
 */
class RecommendedEnumValueActuatorTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringRecommendedEnumValueInspection::class.java)
    }

    fun testDeclaredAccessValueReported() {
        myFixture.configureByText("application.properties", "management.endpoint.conditions.access=READ_ONLY")

        val problems = recommendationProblems()

        assertEquals("expected exactly one report, got: $problems", 1, problems.size)
        assertTrue("expected read-only to be suggested, got: $problems", problems.single().contains("read-only"))
    }

    fun testRecommendedAccessValueNotReported() {
        myFixture.configureByText("application.properties", "management.endpoint.conditions.access=unrestricted")

        val problems = recommendationProblems()

        assertEmpty("Spring's own default spelling must not be reported: $problems", problems)
    }

    fun testHttpExchangesRecordingIncludeDeclaredNameReported() {
        myFixture.configureByText(
            "application.properties",
            "management.httpexchanges.recording.include=REQUEST_HEADERS"
        )

        val problems = recommendationProblems()

        assertEquals("expected exactly one report, got: $problems", 1, problems.size)
        assertTrue("expected request-headers to be suggested, got: $problems",
            problems.single().contains("request-headers"))
    }

    fun testHttpExchangesRecordingIncludeDefaultNotReported() {
        myFixture.configureByText(
            "application.properties",
            "management.httpexchanges.recording.include=time-taken,request-headers,response-headers"
        )

        val problems = recommendationProblems()

        assertEmpty("Spring's own default value must not be reported: $problems", problems)
    }

    private fun recommendationProblems(): List<String> = myFixture.doHighlighting()
        .mapNotNull { it.description }
        .filter { it.contains("Spring writes this enum value") }
}
