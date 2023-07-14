package com.esprito.spring.core.providers

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInspection.deadCode.UnusedDeclarationInspection
import com.intellij.testFramework.TestDataPath
import org.junit.Ignore

private const val TEST_DATA_PATH = "testdata/inspection/suppresses"

@Ignore
@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
class SpringImplicitUsageProviderTest : EspritoInspectionTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testTestConfiguration() = doTest(UnusedDeclarationInspection())
}