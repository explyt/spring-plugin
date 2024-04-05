package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringConfigurationPropertiesNullableParametersInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConfigurationPropertiesNullableParametersInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("nullableConstructorParameters")
    fun testNullableConstructorParameters() = doTest(SpringConfigurationPropertiesNullableParametersInspection())
}