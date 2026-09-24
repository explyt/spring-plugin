/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.properties.references.ValueHintReference
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiEnumConstant

/**
 * An array metadata type (`Include[]`) and a map value type (`Map<String, Include>`) reach the value resolution
 * differently from a collection: the array type never resolved to a class, so it was classified as a scalar, and a map
 * entry key is absent from the metadata, so the exact property lookup failed.
 */
class ArrayAndMapEnumValueReferenceTest : ExplytInspectionJavaTestCase() {
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
    }

    fun testArrayElementResolvesToEnumConstant() =
        assertResolvesToConstant("explyt.recording.array=REQUEST_HEADERS")

    fun testArrayElementResolvesRelaxedKebabCase() =
        assertResolvesToConstant("explyt.recording.array=request-headers", "request-headers")

    fun testArrayElementResolvesRelaxedSnakeCase() =
        assertResolvesToConstant("explyt.recording.array=request_headers", "request_headers")

    fun testMapValueResolvesToEnumConstant() =
        assertResolvesToConstant("explyt.recording.by-name.first=REQUEST_HEADERS")

    fun testMapValueResolvesRelaxedKebabCase() =
        assertResolvesToConstant("explyt.recording.by-name.first=request-headers", "request-headers")

    fun testYamlMapValueResolvesToEnumConstant() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                by-name:
                  first: REQUEST_HEADERS
            """.trimIndent()
        )

        assertEnumConstantResolved("REQUEST_HEADERS")
    }

    fun testYamlArrayElementResolvesToEnumConstant() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                array: REQUEST_HEADERS
            """.trimIndent()
        )

        assertEnumConstantResolved("REQUEST_HEADERS")
    }

    private fun assertResolvesToConstant(text: String, value: String = "REQUEST_HEADERS") {
        myFixture.configureByText("application.properties", text)

        assertEnumConstantResolved(value)
    }

    private fun assertEnumConstantResolved(value: String) {
        val reference = myFixture.file.findReferenceAt(myFixture.file.text.indexOf(value))
        val valueReference = requireNotNull(reference as? ValueHintReference) {
            "expected a ValueHintReference on the value, got: $reference"
        }

        val resolved = valueReference.multiResolve(false).map { it.element }
        assertEquals(ValueHintReference.ResultType.ENUM, valueReference.getResultType())
        assertTrue(
            "the value should resolve to the enum constant, got: $resolved",
            resolved.any { it is PsiEnumConstant && it.name == "REQUEST_HEADERS" }
        )
    }
}
