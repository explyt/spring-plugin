package com.esprito.spring.web.inspections.java

import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.SpringInitBinderNonVoidInspection
import org.jetbrains.kotlin.test.TestMetadata

class SpringInitBinderNonVoidInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("initBinderReturnType")
    fun testInitBinderReturnType() = doTest(SpringInitBinderNonVoidInspection())
}