package com.esprito.spring.core.autoconfigure.inspection

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class EnableAutoConfigureSpringFactoryInspectionTest : EspritoInspectionTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    @TestMetadata("propertyCreateFile")
    fun testPropertyCreateFile() {
        doTest(EnableAutoConfigureSpringFactoryInspection())
    }

    @TestMetadata("propertyMoveToFile")
    fun testPropertyMoveToFile() {
        doTest(EnableAutoConfigureSpringFactoryInspection())
    }

    @TestMetadata("propertyRemove")
    fun testPropertyRemove() {
        doTest(EnableAutoConfigureSpringFactoryInspection())
    }

}
