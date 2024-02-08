package com.esprito.spring.core.inspections.java

import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringAliasNotMetaAnnotatedInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("annotationAliasFor")
    fun testAnnotationAliasFor() = doTest(com.esprito.spring.core.inspections.SpringAliasNotMetaAnnotatedInspection())
}