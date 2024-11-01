package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringKotlinObjectInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(com.explyt.spring.core.inspections.SpringKotlinObjectInspection::class.java)
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
