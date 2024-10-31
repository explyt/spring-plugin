package com.esprito.spring.web.inspections.kotlin

import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.MockMvcTemplateParametersInspection
import org.jetbrains.kotlin.test.TestMetadata

class MockMvcTemplateParametersInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
    )

    @TestMetadata("templateParameters")
    fun testTemplateParameters() = doTest(MockMvcTemplateParametersInspection())
}