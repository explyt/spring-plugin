package com.explyt.spring.web.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.WebClientUriParametersInspection
import org.jetbrains.kotlin.test.TestMetadata

class WebClientUriParametersInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1
    )

    @TestMetadata("uriParameters")
    fun testUriParameters() = doTest(WebClientUriParametersInspection())
}