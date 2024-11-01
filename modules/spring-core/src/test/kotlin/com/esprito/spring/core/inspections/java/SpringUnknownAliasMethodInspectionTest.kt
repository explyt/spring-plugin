package com.explyt.spring.core.inspections.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringUnknownAliasMethodInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("methodsAliasFor")
    fun testMethodsAliasFor() = doTest(com.explyt.spring.core.inspections.SpringUnknownAliasMethodInspection())
}