/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.CodeInsightSettings

/**
 * Value completion has to offer an enum constant for every spelling relaxed binding accepts, because Spring's own
 * metadata ships the dashed lower-case form as the default value: only the declared name was a lookup string, so a
 * user following the framework's own recommendation got "No suggestions".
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
        "REQUEST_HEADERS", "RESPONSE_HEADERS"
    )

    fun testUpperCasePrefixStillOffersConstants() = assertVariants(
        "explyt.recording.include=R<caret>",
        "REQUEST_HEADERS", "RESPONSE_HEADERS"
    )

    fun testEmptyPrefixOffersAllConstants() = assertVariants(
        "explyt.recording.include=<caret>",
        "TIME_TAKEN", "REQUEST_HEADERS", "RESPONSE_HEADERS"
    )

    fun testKebabCasePrefixInsertsDeclaredName() =
        assertInsertsDeclaredName("explyt.recording.include=request-h<caret>")

    fun testSnakeCasePrefixInsertsDeclaredName() =
        assertInsertsDeclaredName("explyt.recording.include=request_h<caret>")

    fun testDeclaredPrefixInsertsDeclaredName() =
        assertInsertsDeclaredName("explyt.recording.include=REQUEST_H<caret>")

    fun testFullKebabCaseValueInsertsDeclaredName() =
        assertInsertsDeclaredName("explyt.recording.include=request-headers<caret>")

    fun testScalarEnumOffersConstantsForLowerCasePrefix() = assertVariants(
        "explyt.recording.single=r<caret>",
        "REQUEST_HEADERS", "RESPONSE_HEADERS"
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

        assertLookupElements("REQUEST_HEADERS", "RESPONSE_HEADERS")
    }

    fun testYamlKebabCasePrefixInsertsDeclaredName() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                include: request-h<caret>
            """.trimIndent()
        )
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            explyt:
              recording:
                include: REQUEST_HEADERS
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

    private fun assertInsertsDeclaredName(text: String) {
        myFixture.configureByText("application.properties", text)
        myFixture.completeBasic()

        myFixture.checkResult("explyt.recording.include=REQUEST_HEADERS")
    }
}
