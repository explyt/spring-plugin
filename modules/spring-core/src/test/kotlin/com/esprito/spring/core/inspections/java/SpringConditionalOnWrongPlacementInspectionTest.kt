package com.esprito.spring.core.inspections.java

import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConditionalOnWrongPlacementInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("conditionalOnWrongPlacement")
    fun testConditionalOnWrongPlacement() = doTest(com.esprito.spring.core.inspections.SpringConditionalOnWrongPlacementInspection())
}