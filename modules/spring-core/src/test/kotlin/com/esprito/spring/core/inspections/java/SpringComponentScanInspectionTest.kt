package com.explyt.spring.core.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringComponentScanInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("componentScan")
    fun testComponentScan() = doTest(com.explyt.spring.core.inspections.SpringComponentScanInvalidPackageInspection())
}