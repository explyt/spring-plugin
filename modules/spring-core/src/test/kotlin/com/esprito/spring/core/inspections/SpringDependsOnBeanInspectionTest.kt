package com.esprito.spring.core.inspections

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "testdata/inspection"

@TestDataPath("\$CONTENT_ROOT/../../$TEST_DATA_PATH")
class SpringDependsOnBeanInspectionTest : EspritoInspectionTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testDependsOn() = doTest(SpringDependsOnBeanInspection())
}