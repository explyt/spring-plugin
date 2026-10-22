/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringRecommendedEnumValueInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Every hint literal Spring ships is written in lower case, so a value that only differs from one by case is a
 * spelling problem with a rewrite, not an error: relaxed binding accepts it and `getProblemValues` deliberately says
 * nothing about a hint whose provider is `any`.
 *
 * A value matching no literal at all stays unreported here — it is either free-form or already owned by
 * [com.explyt.spring.core.inspections.SpringBasePropertyInspection].
 */
class RecommendedHintValueInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringRecommendedEnumValueInspection::class.java)
    }

    fun testUpperCaseLogLevelReported() = assertReported("logging.level.root=INFO", "info")

    fun testMixedCaseLogLevelReported() = assertReported("logging.level.org.springframework=Debug", "debug")

    fun testExactLogLevelNotReported() = assertNotReported("logging.level.root=info")

    fun testUnknownValueNotReported() = assertNotReported("logging.level.root=NOT_A_LEVEL")

    /**
     * A hint without an `any` provider is a closed value set, which the property inspection compares verbatim and
     * reports `READ-ONLY` against `read-only` as an invalid value. Calling that value a mere spelling as well would
     * give it two verdicts, one of them false.
     */
    fun testClosedValueSetIsLeftToTheValueCheck() {
        myFixture.addFileToProject(
            "META-INF/additional-spring-configuration-metadata.json",
            """
            {
              "properties": [{ "name": "explyt.mode", "type": "java.lang.String" }],
              "hints": [{ "name": "explyt.mode", "values": [{ "value": "read-only" }, { "value": "read-write" }] }]
            }
            """.trimIndent()
        )
        myFixture.configureByText("application.properties", "explyt.mode=READ-ONLY")
        val hint = com.explyt.spring.core.util.PropertyUtil.getPropertyHint(module, "explyt.mode")
        assertTrue("Precondition: the closed hint must be loaded", hint?.declaresClosedValueSet() == true)

        assertEmpty("a closed value set belongs to the value check alone", recommendationProblems())
    }

    fun testQuickFixRewritesValue() {
        myFixture.configureByText("application.properties", "logging.level.root=IN<caret>FO")

        applySingleFix()

        myFixture.checkResult("logging.level.root=info")
    }

    fun testYamlUpperCaseLogLevelReported() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                root: INFO
            """.trimIndent()
        )

        val problems = recommendationProblems()

        assertEquals("expected exactly one report, got: $problems", 1, problems.size)
        assertTrue("expected info to be suggested, got: $problems", problems.single().contains("info"))
    }

    fun testYamlQuickFixRewritesValue() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                root: IN<caret>FO
            """.trimIndent()
        )

        applySingleFix()

        myFixture.checkResult(
            """
            logging:
              level:
                root: info
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
        val intentions = myFixture.availableIntentions.filter { it.text.startsWith("Replacement") }
        assertEquals("expected exactly one rewrite fix, got: ${intentions.map { it.text }}", 1, intentions.size)
        myFixture.checkPreviewAndLaunchAction(intentions.single())
    }

    private fun recommendationProblems(): List<String> = myFixture.doHighlighting()
        .mapNotNull { it.description }
        .filter { it.contains("Spring writes this value as") }
}
