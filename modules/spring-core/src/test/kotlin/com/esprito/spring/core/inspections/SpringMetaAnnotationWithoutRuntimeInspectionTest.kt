package com.esprito.spring.core.inspections

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringMetaAnnotationWithoutRuntimeInspectionTest : EspritoInspectionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("retentionPolicy")
    fun testRetentionPolicy() = doTest(SpringMetaAnnotationWithoutRuntimeInspection())
}