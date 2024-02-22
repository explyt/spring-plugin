package com.esprito.spring.security.inspections.java

import com.esprito.spring.security.inspections.SpringSecurityAnnotationWithUserDetailsInspection
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringSecurityAnnotationWithUserDetailsInspectionTest : EspritoInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springSecurityTest_6_0_7
    )

    @TestMetadata("withUserDetails")
    fun testWithUserDetails() = doTest(SpringSecurityAnnotationWithUserDetailsInspection())

}