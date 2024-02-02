package com.esprito.spring.core.inspections

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringAsyncReturnTypeInspectionTest : EspritoInspectionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("asyncReturnType")
    fun testAsyncReturnType() = doTest(SpringAsyncReturnTypeInspection())
}