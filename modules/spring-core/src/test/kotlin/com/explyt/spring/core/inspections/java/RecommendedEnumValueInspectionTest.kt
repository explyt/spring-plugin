/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringRecommendedEnumValueInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Spring's own metadata writes an enum default value in lower-case kebab-case and never in the declared
 * `SCREAMING_SNAKE` form, so the declared form is reported as a style problem with a quick-fix. Relaxed binding
 * accepts every spelling, hence a weak warning rather than an error.
 */
class RecommendedEnumValueInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            package com.explyt;

            public enum RecordingInclude {
                TIME_TAKEN, REQUEST_HEADERS, RESPONSE_HEADERS
            }
            """.trimIndent()
        )
        myFixture.copyFileToProject(
            "collectionEnumValue/META-INF/additional-spring-configuration-metadata.json",
            "META-INF/additional-spring-configuration-metadata.json"
        )
        myFixture.enableInspections(SpringRecommendedEnumValueInspection::class.java)
    }

    fun testDeclaredNameReported() =
        assertReported("explyt.recording.single=REQUEST_HEADERS", "request-headers")

    fun testLowerSnakeCaseReported() =
        assertReported("explyt.recording.single=request_headers", "request-headers")

    fun testMixedCaseDashedFormReported() =
        assertReported("explyt.recording.single=Request-Headers", "request-headers")

    fun testSingleWordConstantReported() =
        assertReported("explyt.recording.single=TIME_TAKEN", "time-taken")

    fun testRecommendedFormNotReported() = assertNotReported("explyt.recording.single=request-headers")

    fun testUnresolvedValueNotReported() = assertNotReported("explyt.recording.single=NOT_A_VALUE")

    fun testPlaceholderNotReported() = assertNotReported("explyt.recording.single=\${recording.include}")

    fun testNonEnumValueNotReported() = assertNotReported("explyt.recording.enabled=true")

    fun testCollectionElementsReportedIndividually() {
        myFixture.configureByText(
            "application.properties",
            "explyt.recording.list=REQUEST_HEADERS,response-headers,TIME_TAKEN"
        )

        val problems = recommendationProblems()

        assertEquals("both non-recommended elements must be reported, got: $problems", 2, problems.size)
        assertTrue("expected request-headers to be suggested, got: $problems",
            problems.any { it.contains("request-headers") })
        assertTrue("expected time-taken to be suggested, got: $problems",
            problems.any { it.contains("time-taken") })
    }

    fun testQuickFixRewritesSingleValue() {
        myFixture.configureByText("application.properties", "explyt.recording.single=REQUEST<caret>_HEADERS")

        applySingleFix()

        myFixture.checkResult("explyt.recording.single=request-headers")
    }

    fun testQuickFixRewritesOnlyTheReportedCollectionElement() {
        myFixture.configureByText(
            "application.properties",
            "explyt.recording.list=RESPONSE<caret>_HEADERS,request-headers"
        )

        applySingleFix()

        myFixture.checkResult("explyt.recording.list=response-headers,request-headers")
    }

    fun testYamlDeclaredNameReported() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                single: REQUEST_HEADERS
            """.trimIndent()
        )

        val problems = recommendationProblems()

        assertTrue("the declared name must be reported in YAML too, got: $problems", problems.isNotEmpty())
    }

    fun testYamlQuickFixRewritesValue() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                single: REQUEST<caret>_HEADERS
            """.trimIndent()
        )

        applySingleFix()

        myFixture.checkResult(
            """
            explyt:
              recording:
                single: request-headers
            """.trimIndent()
        )
    }

    private fun assertReported(propertyLine: String, expectedSuggestion: String) {
        myFixture.configureByText("application.properties", propertyLine)

        val problems = recommendationProblems()

        assertEquals("expected exactly one report, got: $problems", 1, problems.size)
        assertTrue(
            "expected '$expectedSuggestion' to be suggested, got: $problems",
            problems.single().contains(expectedSuggestion)
        )
    }

    private fun assertNotReported(propertyLine: String) {
        myFixture.configureByText("application.properties", propertyLine)

        val problems = recommendationProblems()

        assertEmpty("nothing must be reported here: $problems", problems)
    }

    private fun applySingleFix() {
        val intentions = myFixture.availableIntentions
            .filter { it.text.startsWith("Replacement") }
        assertEquals("expected exactly one rewrite fix, got: ${intentions.map { it.text }}", 1, intentions.size)
        myFixture.checkPreviewAndLaunchAction(intentions.single())
    }

    private fun recommendationProblems(): List<String> = myFixture.doHighlighting()
        .mapNotNull { it.description }
        .filter { it.contains("Spring writes this enum value") }
}
