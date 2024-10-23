package com.esprito.spring.web.inspections.java

import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.OpenFeignInspection
import org.jetbrains.kotlin.test.TestMetadata

class OpenFeignInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springCloud_4_1_3
    )

    @TestMetadata("openFeign")
    fun testOpenFeign() = doTest(OpenFeignInspection())

}