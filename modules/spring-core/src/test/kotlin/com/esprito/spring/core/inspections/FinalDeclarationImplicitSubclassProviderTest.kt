package com.esprito.spring.core.inspections

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInspection.inheritance.ImplicitSubclassInspection
import org.jetbrains.kotlin.test.TestMetadata

class FinalDeclarationImplicitSubclassProviderTest : EspritoInspectionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    @TestMetadata("finalDeclaration")
    fun testFinalDeclaration() = doTest(ImplicitSubclassInspection())
}