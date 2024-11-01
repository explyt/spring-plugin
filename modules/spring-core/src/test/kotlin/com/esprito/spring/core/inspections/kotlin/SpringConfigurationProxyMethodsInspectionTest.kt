package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringConfigurationProxyMethodsInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringConfigurationProxyMethodsInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    @TestMetadata("configurationProxyMethods")
    fun testConfigurationProxyMethods() = doTest(SpringConfigurationProxyMethodsInspection())
}