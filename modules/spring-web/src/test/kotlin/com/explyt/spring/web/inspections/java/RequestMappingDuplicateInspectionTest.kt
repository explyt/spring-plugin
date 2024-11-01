package com.explyt.spring.web.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.RequestMappingDuplicateInspection
import org.jetbrains.kotlin.test.TestMetadata

class RequestMappingDuplicateInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springWeb_6_0_7)

    @TestMetadata("duplicatedEndpoint")
    fun testDuplicatedEndpoint() = doTest(RequestMappingDuplicateInspection())
}