package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringConfigurationPropertiesNullableParametersInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConfigurationPropertiesNullableParametersInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("nullableConstructorParameters")
    fun testNullableConstructorParameters() = doTest(SpringConfigurationPropertiesNullableParametersInspection())
}