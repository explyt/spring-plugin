package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringValueAnnotationInspection
import com.explyt.spring.core.inspections.quickfix.ReplacementStringQuickFix
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import junit.framework.TestCase

class SpringValueAnnotationInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringValueAnnotationInspection::class.java)
    }

    fun testValidValue() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        val value = "\\\${test.property}"
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}           
            class SpringBean {
                @${SpringCoreClasses.VALUE}("$value") var testField = "test"
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValidStringValue() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}            
            class SpringBean {
                @${SpringCoreClasses.VALUE}("test.string.value") var testField = "test"
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testInvalidValue() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}
            
            public class SpringBean {
                @${SpringCoreClasses.VALUE}(<warning>"test.property"</warning>) var testField = "test"
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testQuickFix() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}           
            class SpringBean {
                @${SpringCoreClasses.VALUE}(<warning>"test.property"</warning>) var testField = "test"
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
        val quickFix = myFixture.getAllQuickFixes().filterIsInstance<ReplacementStringQuickFix>().firstOrNull()
        TestCase.assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        val value = "\\\${test.property}"
        myFixture.checkResult(
            """
            @org.springframework.stereotype.Component
            class SpringBean {
                @org.springframework.beans.factory.annotation.Value("$value") var testField = "test"
            }
        """.trimIndent(), true
        )
    }
}
