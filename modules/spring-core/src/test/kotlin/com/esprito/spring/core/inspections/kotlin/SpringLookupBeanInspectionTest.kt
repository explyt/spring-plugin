package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringLookupBeanInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringLookupBeanInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("lookup")
    fun testLookup() = doTest(SpringLookupBeanInspection())
}