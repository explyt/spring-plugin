package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringEventListenerInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringEventListenerInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("eventListener")
    fun testEventListener() = doTest(SpringEventListenerInspection())
}