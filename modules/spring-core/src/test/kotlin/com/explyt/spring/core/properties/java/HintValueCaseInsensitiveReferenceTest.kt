/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral

/**
 * A metadata hint literal is matched under Spring's relaxed binding, not verbatim.
 *
 * `logging.level.*` is bound as `Map<String, LogLevel>`, so `INFO`, `Info` and `info` are the same value at runtime,
 * yet only the exactly written literal used to navigate to the hint declaration.
 */
class HintValueCaseInsensitiveReferenceTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBoot_3_1_1)

    fun testUpperCaseValueResolvesToHintLiteral() = assertResolvesToHintLiteral("INFO")

    fun testMixedCaseValueResolvesToHintLiteral() = assertResolvesToHintLiteral("Info")

    fun testExactValueStillResolvesToHintLiteral() = assertResolvesToHintLiteral("info")

    fun testUnknownValueDoesNotResolve() {
        myFixture.configureByText("application.properties", "logging.level.root=NOT_A_LEVEL")

        val reference = myFixture.file.findReferenceAt(myFixture.file.text.indexOf("NOT_A_LEVEL"))
        val valueReference = reference as? ValueHintReference

        assertTrue(
            "a value matching no hint literal must not resolve",
            valueReference == null || valueReference.multiResolve(false).isEmpty()
        )
    }

    fun testYamlUpperCaseValueResolvesToHintLiteral() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                root: INFO
            """.trimIndent()
        )

        assertHintLiteralResolved("INFO", "info")
    }

    private fun assertResolvesToHintLiteral(written: String) {
        myFixture.configureByText("application.properties", "logging.level.root=$written")

        assertHintLiteralResolved(written, written.lowercase())
    }

    private fun assertHintLiteralResolved(written: String, expectedLiteral: String) {
        val reference = myFixture.file.findReferenceAt(myFixture.file.text.indexOf(written))
        val valueReference = requireNotNull(reference as? ValueHintReference) {
            "expected a ValueHintReference on the value, got: $reference"
        }

        val resolved = valueReference.multiResolve(false).mapNotNull { it.element }
        val literals = resolved.filterIsInstance<JsonProperty>()
            .mapNotNull { (it.value as? JsonStringLiteral)?.value }

        assertEquals(
            "one hint declaration is the useful target, got: ${resolved.map { it.containingFile.virtualFile.path }}",
            1, resolved.size
        )
        assertEquals(
            "the written value must navigate to the hint literal, got: $literals",
            listOf(expectedLiteral), literals
        )
        assertEquals(ValueHintReference.ResultType.METADATA, valueReference.getResultType())
    }
}
