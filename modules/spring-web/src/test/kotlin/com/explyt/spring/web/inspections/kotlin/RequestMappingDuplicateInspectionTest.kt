package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.RequestMappingDuplicateInspection
import org.jetbrains.kotlin.test.TestMetadata

class RequestMappingDuplicateInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("duplicatedEndpoint")
    fun testDuplicatedEndpoint() = doTest(RequestMappingDuplicateInspection())
}