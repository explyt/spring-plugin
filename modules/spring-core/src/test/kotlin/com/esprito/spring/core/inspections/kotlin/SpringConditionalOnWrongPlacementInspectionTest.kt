package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringConditionalOnWrongPlacementInspection
import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConditionalOnWrongPlacementInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("conditionalOnWrongPlacement")
    fun testConditionalOnWrongPlacement() = doTest(SpringConditionalOnWrongPlacementInspection())
}