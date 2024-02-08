package com.esprito.spring.core.inspections.java

import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringMetaAnnotationWithoutRuntimeInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("retentionPolicy")
    fun testRetentionPolicy() = doTest(com.esprito.spring.core.inspections.SpringMetaAnnotationWithoutRuntimeInspection())
}