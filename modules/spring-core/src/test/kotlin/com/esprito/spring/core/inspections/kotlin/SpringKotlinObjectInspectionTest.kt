package com.esprito.spring.core.inspections.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary

class SpringKotlinObjectInspectionTest : EspritoInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(com.esprito.spring.core.inspections.SpringKotlinObjectInspection::class.java)
    }

    fun testCorrectComponent() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            class SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testObjectComponent() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            <warning>object</warning> SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

}
