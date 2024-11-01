package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringYamlInspectionTest : ExplytInspectionJavaTestCase() {

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
    <warning descr="Cannot resolve key property 'foo.barBaz'"><warning descr="Should be kebab-case">barBaz</warning></warning>: some1
    <warning descr="Cannot resolve key property 'foo.bar-Baz1'"><warning descr="Should be kebab-case">bar-Baz1</warning></warning>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }
}