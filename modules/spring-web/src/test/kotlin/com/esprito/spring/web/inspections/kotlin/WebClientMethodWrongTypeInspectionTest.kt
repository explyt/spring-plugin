package com.esprito.spring.web.inspections.kotlin

import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.WebClientMethodWrongTypeInspection
import org.jetbrains.kotlin.test.TestMetadata

class WebClientMethodWrongTypeInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.kotlin_coroutines_1_7_1
    )

    @TestMetadata("wrongType")
    fun testWrongType() = doTest(WebClientMethodWrongTypeInspection())

}