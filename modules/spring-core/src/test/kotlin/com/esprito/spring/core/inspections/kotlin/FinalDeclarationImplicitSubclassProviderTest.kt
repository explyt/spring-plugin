package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInspection.inheritance.ImplicitSubclassInspection
import org.jetbrains.kotlin.test.TestMetadata

class FinalDeclarationImplicitSubclassProviderTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTx_6_0_7
    )

    @TestMetadata("finalDeclaration")
    fun testFinalDeclaration() = doTest(ImplicitSubclassInspection())
}