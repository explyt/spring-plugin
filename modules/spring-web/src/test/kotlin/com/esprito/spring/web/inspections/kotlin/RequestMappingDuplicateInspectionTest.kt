package com.esprito.spring.web.inspections.kotlin

import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.web.inspections.RequestMappingDuplicateInspection
import org.jetbrains.kotlin.test.TestMetadata

class RequestMappingDuplicateInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("duplicatedEndpoint")
    fun testDuplicatedEndpoint() = doTest(RequestMappingDuplicateInspection())
}