package com.esprito.spring.core.inspections

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConditionalOnWrongPlacementInspectionTest : EspritoInspectionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("conditionalOnWrongPlacement")
    fun testConditionalOnWrongPlacement() = doTest(SpringConditionalOnWrongPlacementInspection())
}