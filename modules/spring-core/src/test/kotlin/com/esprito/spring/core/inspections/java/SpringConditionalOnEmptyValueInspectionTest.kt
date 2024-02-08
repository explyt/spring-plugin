package com.esprito.spring.core.inspections.java

import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConditionalOnEmptyValueInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("conditionalOnPropertyValue")
    fun testConditionalOnPropertyValue() = doTest(com.esprito.spring.core.inspections.SpringConditionalOnEmptyValueInspection())
}