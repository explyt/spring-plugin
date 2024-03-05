package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.PsiFileReferenceInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class PsiFileReferenceInspectionTest : EspritoInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7
    )

    @TestMetadata("fileResource")
    fun testFileResource() = doTest(PsiFileReferenceInspection())

}