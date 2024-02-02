package com.esprito.spring.web.inspections

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringInitBinderNonVoidInspectionTest : EspritoInspectionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("initBinderReturnType")
    fun testInitBinderReturnType() = doTest(SpringInitBinderNonVoidInspection())
}