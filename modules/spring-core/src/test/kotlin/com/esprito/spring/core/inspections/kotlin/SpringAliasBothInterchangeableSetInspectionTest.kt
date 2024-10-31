package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringAliasBothInterchangeableSetInspection
import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringAliasBothInterchangeableSetInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("interchangeableAliasFor")
    fun testInterchangeableAliasFor() = doTest(SpringAliasBothInterchangeableSetInspection())
}