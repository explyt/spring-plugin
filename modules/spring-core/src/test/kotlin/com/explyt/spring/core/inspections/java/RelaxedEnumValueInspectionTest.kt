/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Spring's relaxed binding accepts an enum constant in any case with `-` and `_` interchangeable, and Spring's own
 * metadata ships the dashed form as a default value, so reporting it as unresolved flags the spelling the framework
 * recommends. The inspection and [com.explyt.spring.core.properties.references.ValueHintReference] share the same
 * matcher, so they cannot disagree about whether a value is valid.
 */
class RelaxedEnumValueInspectionTest : ExplytInspectionJavaTestCase() {
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
        myFixture.enableInspections(SpringPropertiesInspection::class.java)
    }

    fun testDashedFormNotReported() = assertNotReported("explyt.recording.include=request-headers")

    fun testLowerCaseUnderscoreFormNotReported() = assertNotReported("explyt.recording.include=request_headers")

    fun testMixedCaseDashedFormNotReported() = assertNotReported("explyt.recording.include=Request-Headers")

    fun testCanonicalFormNotReported() = assertNotReported("explyt.recording.include=REQUEST_HEADERS")

    fun testScalarEnumDashedFormNotReported() = assertNotReported("explyt.recording.single=time-taken")

    fun testInvalidValueStillReported() {
        myFixture.configureByText("application.properties", "explyt.recording.include=NOT_A_VALUE")

        val problems = enumProblems()

        assertTrue("an invalid constant must still be reported, got: $problems", problems.isNotEmpty())
    }

    fun testYamlDashedFormNotReported() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                include: request-headers
            """.trimIndent()
        )

        val problems = enumProblems()

        assertEmpty("the dashed form must not be reported: $problems", problems)
    }

    private fun assertNotReported(propertyLine: String) {
        myFixture.configureByText("application.properties", propertyLine)

        val problems = enumProblems()

        assertEmpty("a relaxed enum form must not be reported: $problems", problems)
    }

    private fun enumProblems(): List<String> = myFixture.doHighlighting()
        .mapNotNull { it.description }
        .filter { it.contains("RecordingInclude") }
}
