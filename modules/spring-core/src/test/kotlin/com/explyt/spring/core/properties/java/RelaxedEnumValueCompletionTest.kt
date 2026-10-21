/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.CodeInsightSettings

/**
 * Value completion has to offer an enum constant for every spelling relaxed binding accepts and insert the one Spring
 * itself recommends: its own metadata ships enum default values in lower-case kebab-case (`never`, `read-uncommitted`)
 * and never in the declared `SCREAMING_SNAKE` form.
 */
class RelaxedEnumValueCompletionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1
    )

    private var savedCompletionCaseSensitive = CodeInsightSettings.FIRST_LETTER

    override fun setUp() {
        super.setUp()
        // The lower-case prefix would match regardless of the fix under CodeInsightSettings.NONE, so pin the
        // production default instead of trusting the test environment.
        val settings = CodeInsightSettings.getInstance()
        savedCompletionCaseSensitive = settings.completionCaseSensitive
        settings.completionCaseSensitive = CodeInsightSettings.FIRST_LETTER
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
    }

    override fun tearDown() {
        try {
            CodeInsightSettings.getInstance().completionCaseSensitive = savedCompletionCaseSensitive
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testLowerCasePrefixOffersConstants() = assertVariants(
        "explyt.recording.include=r<caret>",
        "request-headers", "response-headers"
    )

    fun testUpperCasePrefixStillOffersConstants() = assertVariants(
        "explyt.recording.include=R<caret>",
        "request-headers", "response-headers"
    )

    fun testEmptyPrefixOffersAllConstants() = assertVariants(
        "explyt.recording.include=<caret>",
        "time-taken", "request-headers", "response-headers"
    )

    fun testKebabCasePrefixInsertsRecommendedForm() =
        assertInsertsRecommendedForm("explyt.recording.include=request-h<caret>")

    fun testSnakeCasePrefixInsertsRecommendedForm() =
        assertInsertsRecommendedForm("explyt.recording.include=request_h<caret>")

    fun testDeclaredPrefixInsertsRecommendedForm() =
        assertInsertsRecommendedForm("explyt.recording.include=REQUEST_H<caret>")

    fun testDeclaredNamePrefixStillMatches() =
        assertInsertsRecommendedForm("explyt.recording.include=REQ<caret>")

    fun testFullKebabCaseValueInsertsRecommendedForm() =
        assertInsertsRecommendedForm("explyt.recording.include=request-headers<caret>")

    fun testScalarEnumOffersConstantsForLowerCasePrefix() = assertVariants(
        "explyt.recording.single=r<caret>",
        "request-headers", "response-headers"
    )

    fun testNonEnumValueTypeIsUnaffected() = assertVariants(
        "explyt.recording.enabled=<caret>",
        "true", "false"
    )

    fun testYamlLowerCasePrefixOffersConstants() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                include: r<caret>
            """.trimIndent()
        )

        assertLookupElements("request-headers", "response-headers")
    }

    fun testYamlDeclaredPrefixInsertsRecommendedForm() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                include: REQUEST_H<caret>
            """.trimIndent()
        )
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            explyt:
              recording:
                include: request-headers
            """.trimIndent()
        )
    }

    private fun assertVariants(text: String, vararg expected: String) {
        myFixture.configureByText("application.properties", text)

        assertLookupElements(*expected)
    }

    private fun assertLookupElements(vararg expected: String) {
        myFixture.completeBasic()
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull("expected a completion popup, got a single auto-inserted item", lookupElementStrings)
        assertEquals(expected.toSet(), lookupElementStrings!!.toSet())
    }

    private fun assertInsertsRecommendedForm(text: String) {
        myFixture.configureByText("application.properties", text)
        myFixture.completeBasic()

        myFixture.checkResult("explyt.recording.include=request-headers")
    }
}
