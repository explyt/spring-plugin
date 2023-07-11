package com.esprito.spring.core.providers

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection

class SpringImplicitUsageProviderTest : EspritoInspectionTestCase() {

    override fun getTestDataPath(): String = "testdata/inspection/suppresses"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext6)

    fun testTestConfiguration() = doTest(UnusedDeclarationInspection())
}