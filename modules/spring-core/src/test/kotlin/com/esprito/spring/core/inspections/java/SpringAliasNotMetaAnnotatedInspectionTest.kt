package com.explyt.spring.core.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringAliasNotMetaAnnotatedInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("annotationAliasFor")
    fun testAnnotationAliasFor() = doTest(com.explyt.spring.core.inspections.SpringAliasNotMetaAnnotatedInspection())
}