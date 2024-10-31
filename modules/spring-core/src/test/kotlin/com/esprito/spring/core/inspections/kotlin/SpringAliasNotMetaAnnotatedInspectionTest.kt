package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringAliasNotMetaAnnotatedInspection
import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringAliasNotMetaAnnotatedInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("annotationAliasFor")
    fun testAnnotationAliasFor() = doTest(SpringAliasNotMetaAnnotatedInspection())
}