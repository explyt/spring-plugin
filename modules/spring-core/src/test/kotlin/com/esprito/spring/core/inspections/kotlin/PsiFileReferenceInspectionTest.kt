package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.PsiFileReferenceInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class PsiFileReferenceInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7
    )

    @TestMetadata("fileResource")
    fun testFileResource() = doTest(PsiFileReferenceInspection())

}