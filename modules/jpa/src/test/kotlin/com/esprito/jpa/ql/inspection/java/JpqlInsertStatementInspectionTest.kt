package com.esprito.jpa.ql.inspection.java

import com.esprito.jpa.ql.inspection.JpqlInsertStatementInspection
import com.esprito.spring.test.ExplytInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class JpqlInsertStatementInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.jakarta_persistence_3_1_0
    )

    override val realJdk = true

    @TestMetadata("insertStatement")
    fun testInsertStatement() = doTest(JpqlInsertStatementInspection())
}