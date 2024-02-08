package com.esprito.spring.core.inspections.java

import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringComponentScanInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("componentScan")
    fun testComponentScan() = doTest(com.esprito.spring.core.inspections.SpringComponentScanInvalidPackageInspection())
}