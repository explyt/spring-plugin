package com.esprito.spring.web.inspections.kotlin

import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.SpringOmittedPathVariableParameterInspection
import org.jetbrains.kotlin.test.TestMetadata

class SpringOmittedPathVariableParameterInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("pathVariableController")
    fun testPathVariableController() = doTest(SpringOmittedPathVariableParameterInspection())
}