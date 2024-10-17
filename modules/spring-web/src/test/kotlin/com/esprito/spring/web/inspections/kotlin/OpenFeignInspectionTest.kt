package com.esprito.spring.web.inspections.kotlin

import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.OpenFeignInspection
import org.jetbrains.kotlin.test.TestMetadata

class OpenFeignInspectionTest : EspritoInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springCloud_4_1_3
    )

    @TestMetadata("openFeign")
    fun testOpenFeign() = doTest(OpenFeignInspection())
}