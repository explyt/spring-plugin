package com.esprito.spring.web.inspections.java

import com.esprito.spring.test.ExplytInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.SpringOmittedPathVariableParameterInspection
import org.jetbrains.kotlin.test.TestMetadata

class SpringOmittedPathVariableParameterInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("pathVariableController")
    fun testPathVariableController() = doTest(SpringOmittedPathVariableParameterInspection())
}