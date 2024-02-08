package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.inspections.SpringConfigurationProxyMethodsInspection
import com.esprito.spring.test.EspritoInspectionKotlinTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConfigurationProxyMethodsInspectionTest : EspritoInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("configurationProxyMethods")
    fun testConfigurationProxyMethods() = doTest(SpringConfigurationProxyMethodsInspection())
}