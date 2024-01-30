package com.esprito.jpa.ql.inspection

import com.esprito.spring.test.EspritoInspectionTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class JpqlFullyQualifiedConstructorInspectionTest : EspritoInspectionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.jakarta_persistence_3_1_0
    )

    override val realJdk = true

    @TestMetadata("constructorExpression")
    fun testConstructorExpression() = doTest(JpqlFullyQualifiedConstructorInspection())
}