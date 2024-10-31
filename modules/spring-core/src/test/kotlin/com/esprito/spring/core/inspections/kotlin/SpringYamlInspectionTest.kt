package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringYamlInspection
import com.esprito.spring.test.ExplytInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringYamlInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    @TestMetadata("yaml")
    fun testYaml() = doTest(SpringYamlInspection())
}