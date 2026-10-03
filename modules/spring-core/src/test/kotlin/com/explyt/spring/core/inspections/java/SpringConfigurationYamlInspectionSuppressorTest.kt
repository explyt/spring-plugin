/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool

class SpringConfigurationYamlInspectionSuppressorTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(yamlIncompatibleTypesInspection())
    }

    fun testMixedScalarTypesInSpringConfigurationAreNotReported() {
        myFixture.configureByText("application.yaml", mixedScalarTypes())
        myFixture.testHighlighting("application.yaml")
    }

    fun testMixedScalarTypesOutsideSpringConfigurationAreStillReported() {
        myFixture.configureByText(
            "pipeline.yaml",
            mixedScalarTypes(
                placeholder = "<warning descr=\"The type of value is 'string' while other values use type 'boolean'\">" +
                        "$PLACEHOLDER</warning>"
            )
        )
        myFixture.testHighlighting("pipeline.yaml")
    }

    private fun mixedScalarTypes(placeholder: String = PLACEHOLDER) = """
explyt:
  sources:
    - enabled: true
    - enabled: false
    - enabled: $placeholder
    """.trimIndent()

    private fun yamlIncompatibleTypesInspection(): LocalInspectionTool =
        LocalInspectionEP.LOCAL_INSPECTION.extensionList
            .single { it.shortName == "YAMLIncompatibleTypes" }
            .instantiateTool() as LocalInspectionTool

    private companion object {
        const val PLACEHOLDER = $$"\"${FLAG:true}\""
    }
}
