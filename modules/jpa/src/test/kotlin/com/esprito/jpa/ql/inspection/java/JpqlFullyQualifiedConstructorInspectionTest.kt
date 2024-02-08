package com.esprito.jpa.ql.inspection.java

import com.esprito.jpa.ql.inspection.JpqlFullyQualifiedConstructorInspection
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class JpqlFullyQualifiedConstructorInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.jakarta_persistence_3_1_0
    )

    override val realJdk = true

    @TestMetadata("constructorExpression")
    fun testConstructorExpression() = doTest(JpqlFullyQualifiedConstructorInspection())
}