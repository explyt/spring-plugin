package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringProfileWrongPlacementInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringProfileWrongPlacementInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("profileWrongPlacement")
    fun testProfileWrongPlacement() = doTest(SpringProfileWrongPlacementInspection())
}