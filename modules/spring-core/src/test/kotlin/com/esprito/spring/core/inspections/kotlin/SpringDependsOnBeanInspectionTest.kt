package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringDependsOnBeanInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringDependsOnBeanInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("dependsOn")
    fun testDependsOn() = doTest(SpringDependsOnBeanInspection())
}