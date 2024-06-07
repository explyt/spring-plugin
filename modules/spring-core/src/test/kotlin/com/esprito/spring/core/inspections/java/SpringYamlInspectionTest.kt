package com.esprito.spring.core.inspections.java

import com.esprito.spring.core.inspections.SpringYamlInspection
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringYamlInspectionTest : EspritoInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    @TestMetadata("yaml")
    fun testYaml() = doTest(SpringYamlInspection())

    fun testDuplicateProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <error>barBaz</error>: some1
    <error>bar-Baz</error>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testNoDuplicateProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <warning descr="Cannot resolve key property 'foo.barBaz'">barBaz</warning>: some1
    <warning descr="Cannot resolve key property 'foo.bar-Baz1'">bar-Baz1</warning>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }
}