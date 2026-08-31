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
 * A metadata `type` such as `java.util.Set<...Include>` names the container, so the element type has to be unwrapped
 * before the value can be resolved against the enum. The inspection already did that, the reference did not, which is
 * why a valid constant was never highlighted as an enum value.
 */
class CollectionEnumValueReferenceTest : ExplytInspectionJavaTestCase() {
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

    fun testSetElementResolvesToEnumConstant() = assertResolvesToConstant("explyt.recording.include")

    fun testListElementResolvesToEnumConstant() = assertResolvesToConstant("explyt.recording.list")

    fun testCollectionElementResolvesToEnumConstant() = assertResolvesToConstant("explyt.recording.collection")


    fun testScalarEnumStillResolves() = assertResolvesToConstant("explyt.recording.single")

    fun testLowerCaseUnderscoreFormResolves() =
        assertResolvesToConstant("explyt.recording.include", "request_headers")

    fun testDashedFormResolves() =
        assertResolvesToConstant("explyt.recording.include", "request-headers")

    fun testMixedCaseDashedFormResolves() =
        assertResolvesToConstant("explyt.recording.include", "Request-Headers")

    fun testScalarEnumResolvesDashedForm() =
        assertResolvesToConstant("explyt.recording.single", "request-headers")

    fun testUnknownValueDoesNotResolve() {
        myFixture.configureByText("application.properties", "explyt.recording.include=NOT_A_VALUE")

        val reference = myFixture.file.findReferenceAt(myFixture.file.text.indexOf("NOT_A_VALUE"))
        val valueReference = reference as? ValueHintReference
        assertTrue(
            "an unknown value must not resolve to a constant",
            valueReference == null || valueReference.multiResolve(false).isEmpty()
        )
    }

    fun testYamlSetElementResolvesToEnumConstant() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                include: REQUEST_HEADERS
            """.trimIndent()
        )

        assertEnumConstantResolved()
    }

    fun testYamlDashedFormResolves() {
        myFixture.configureByText(
            "application.yaml",
            """
            explyt:
              recording:
                include: request-headers
            """.trimIndent()
        )

        assertEnumConstantResolved("request-headers")
    }

    private fun assertResolvesToConstant(key: String, value: String = "REQUEST_HEADERS") {
        myFixture.configureByText("application.properties", "$key=$value")

        assertEnumConstantResolved(value)
    }

    private fun assertEnumConstantResolved(value: String = "REQUEST_HEADERS") {
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
