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

    private fun assertResolvesToConstant(key: String) {
        myFixture.configureByText("application.properties", "$key=REQUEST_HEADERS")

        assertEnumConstantResolved()
    }

    private fun assertEnumConstantResolved() {
        val reference = myFixture.file.findReferenceAt(myFixture.file.text.indexOf("REQUEST_HEADERS"))
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
