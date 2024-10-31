package com.esprito.spring.web.inspections.java

import com.esprito.spring.test.ExplytInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.MockMvcTemplateParametersInspection
import org.jetbrains.kotlin.test.TestMetadata

class MockMvcTemplateParametersInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
    )

    @TestMetadata("templateParameters")
    fun testTemplateParameters() = doTest(MockMvcTemplateParametersInspection())
}