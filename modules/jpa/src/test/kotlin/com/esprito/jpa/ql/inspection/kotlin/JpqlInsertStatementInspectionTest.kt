package com.esprito.jpa.ql.inspection.kotlin

import com.esprito.jpa.ql.inspection.JpqlInsertStatementInspection
import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class JpqlInsertStatementInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.jakarta_persistence_3_1_0
    )

    override val realJdk = true

    @TestMetadata("insertStatement")
    fun testInsertStatement() = doTest(JpqlInsertStatementInspection())
}