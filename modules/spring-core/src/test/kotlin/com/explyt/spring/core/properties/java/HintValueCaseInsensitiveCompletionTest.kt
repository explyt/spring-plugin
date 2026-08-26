/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.CodeInsightSettings

/**
 * Value completion has to offer a metadata hint literal for every case relaxed binding accepts, and still insert the
 * literal Spring ships. `logging.level.values` declares `trace`/`debug`/`info`/..., all lower case, while the level a
 * user types is habitually upper case.
 */
class HintValueCaseInsensitiveCompletionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBoot_3_1_1)

    private var savedCompletionCaseSensitive = CodeInsightSettings.FIRST_LETTER

    override fun setUp() {
        super.setUp()
        // An upper-case prefix would match regardless of the fix under CodeInsightSettings.NONE, so pin the
        // production default instead of trusting the test environment.
        val settings = CodeInsightSettings.getInstance()
        savedCompletionCaseSensitive = settings.completionCaseSensitive
        settings.completionCaseSensitive = CodeInsightSettings.FIRST_LETTER
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

    fun testUpperCasePrefixInsertsDeclaredLiteral() =
        assertCompletesTo("logging.level.org.springframework=IN", "logging.level.org.springframework=info")

    fun testMixedCasePrefixInsertsDeclaredLiteral() =
        assertCompletesTo("logging.level.org.springframework=In", "logging.level.org.springframework=info")

    fun testFullUpperCaseValueInsertsDeclaredLiteral() =
        assertCompletesTo("logging.level.org.springframework=INFO", "logging.level.org.springframework=info")

    fun testLowerCasePrefixStillInsertsDeclaredLiteral() =
        assertCompletesTo("logging.level.org.springframework=in", "logging.level.org.springframework=info")

    fun testUpperCasePrefixInsertsTheLiteralItMatches() =
        assertCompletesTo("logging.level.root=WA", "logging.level.root=warn")

    fun testEmptyPrefixOffersEveryDeclaredLiteral() {
        myFixture.configureByText("application.properties", "logging.level.root=<caret>")
        myFixture.completeBasic()

        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull("expected a completion popup, but got a single auto-inserted item", lookupElementStrings)
        assertEquals(
            setOf("trace", "debug", "info", "warn", "error", "fatal", "off"),
            lookupElementStrings!!.toSet()
        )
    }

    fun testPrefixMatchingNoLiteralOffersNothing() {
        myFixture.configureByText("application.properties", "logging.level.root=ZZ<caret>")
        myFixture.completeBasic()

        assertTrue(
            "no hint literal starts with the typed prefix, got: ${myFixture.lookupElementStrings}",
            myFixture.lookupElementStrings.isNullOrEmpty()
        )
        myFixture.checkResult("logging.level.root=ZZ")
    }

    fun testYamlUpperCasePrefixInsertsDeclaredLiteral() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                root: IN<caret>
            """.trimIndent()
        )
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            logging:
              level:
                root: info
            """.trimIndent()
        )
    }

    fun testYamlLowerCasePrefixStillInsertsDeclaredLiteral() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                root: in<caret>
            """.trimIndent()
        )
        myFixture.completeBasic()

        myFixture.checkResult(
            """
            logging:
              level:
                root: info
            """.trimIndent()
        )
    }

    private fun assertCompletesTo(text: String, expected: String) {
        myFixture.configureByText("application.properties", "$text<caret>")
        myFixture.completeBasic()

        myFixture.checkResult(expected)
    }
}
