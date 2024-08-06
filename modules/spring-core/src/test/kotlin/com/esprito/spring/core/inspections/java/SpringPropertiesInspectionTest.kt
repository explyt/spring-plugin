package com.esprito.spring.core.inspections.java

import com.esprito.spring.core.inspections.SpringPropertiesInspection
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringPropertiesInspectionTest : EspritoInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringPropertiesInspection::class.java)
    }

    @TestMetadata("properties")
    fun testProperties() = doTest(SpringPropertiesInspection())

    fun testDuplicateProperties() {
        myFixture.configureByText(
            "application.properties",
            """
            <error>test.fooBar</error>=1
            <error>test.foo-bar</error>=2
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testNoDuplicateProperties() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="Cannot resolve key property 'test.fooBar'"><warning descr="Should be kebab-case">test.fooBar</warning></warning>=1
            <warning descr="Cannot resolve key property 'test.foo-bar1'">test.foo-bar1</warning>=2
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }
}
