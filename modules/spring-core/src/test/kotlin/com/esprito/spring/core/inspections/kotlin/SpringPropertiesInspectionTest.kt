package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringPropertiesInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringPropertiesInspectionTest : EspritoInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    @TestMetadata("properties")
    fun testProperties() = doTest(SpringPropertiesInspection())

}
