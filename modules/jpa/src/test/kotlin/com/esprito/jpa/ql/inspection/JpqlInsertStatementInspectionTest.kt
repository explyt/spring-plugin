package com.esprito.jpa.ql.inspection

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.testFramework.TestDataPath

@TestDataPath(JpqlInsertStatementInspectionTest.TEST_DATA_PATH)
class JpqlInsertStatementInspectionTest : EspritoInspectionTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.jakarta_persistence_3_1_0
    )

    override val realJdk = true

    fun testInsertStatement() = doTest(JpqlInsertStatementInspection())

    companion object {
        const val TEST_DATA_PATH = "src/test/testdata/inspection"
    }
}