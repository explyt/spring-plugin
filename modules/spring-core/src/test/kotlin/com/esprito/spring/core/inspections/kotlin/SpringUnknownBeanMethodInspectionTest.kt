package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringUnknownBeanMethodInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringUnknownBeanMethodInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("methodsInitDestroy")
    fun testMethodsInitDestroy() = doTest(SpringUnknownBeanMethodInspection())
}