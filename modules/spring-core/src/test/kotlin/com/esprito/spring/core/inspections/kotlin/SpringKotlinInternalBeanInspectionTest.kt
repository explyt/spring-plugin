package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringKotlinInternalBeanInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

class SpringKotlinInternalBeanInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringKotlinInternalBeanInspection::class.java)
    }

    fun testSimpleBeanWithoutInternal() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.BEAN}
                fun service() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testInternalBean() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.BEAN}
                internal fun <warning>service</warning>() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }
}
