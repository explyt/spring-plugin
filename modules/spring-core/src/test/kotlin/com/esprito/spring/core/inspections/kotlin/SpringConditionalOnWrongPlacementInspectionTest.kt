package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringConditionalOnWrongPlacementInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConditionalOnWrongPlacementInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("conditionalOnWrongPlacement")
    fun testConditionalOnWrongPlacement() = doTest(SpringConditionalOnWrongPlacementInspection())
}