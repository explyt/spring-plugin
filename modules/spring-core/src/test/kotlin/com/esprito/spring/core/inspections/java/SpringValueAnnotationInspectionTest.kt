package com.esprito.spring.core.inspections.java

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.inspections.SpringValueAnnotationInspection
import com.esprito.spring.core.inspections.quickfix.ReplacementStringQuickFix
import com.esprito.spring.test.EspritoInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

class SpringValueAnnotationInspectionTest : EspritoInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringValueAnnotationInspection::class.java)
    }

    fun testValidValue() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        val value = "\${test.property}"
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringBean {
                @${SpringCoreClasses.VALUE}("$value") String testField;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValidStringValue() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringBean {
                @${SpringCoreClasses.VALUE}("test.string.value") String testField;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testInvalidValue() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}            
            public class SpringBean {
                @${SpringCoreClasses.VALUE}(<warning>"test.property"</warning>) String testField;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testQuickFix() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            public class SpringBean {
                @${SpringCoreClasses.VALUE}(<warning>"test.property"</warning>) String testField;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
        val quickFix = myFixture.getAllQuickFixes().filterIsInstance<ReplacementStringQuickFix>().firstOrNull()
        TestCase.assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        val value = "\${test.property}"
        myFixture.checkResult(
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                @org.springframework.beans.factory.annotation.Value("$value") String testField;
            }
        """.trimIndent(), true
        )
    }
}
