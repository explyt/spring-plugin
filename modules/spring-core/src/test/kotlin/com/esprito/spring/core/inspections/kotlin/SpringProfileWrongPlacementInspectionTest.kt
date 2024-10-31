package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringProfileWrongPlacementInspection
import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringProfileWrongPlacementInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("profileWrongPlacement")
    fun testProfileWrongPlacement() = doTest(SpringProfileWrongPlacementInspection())
}