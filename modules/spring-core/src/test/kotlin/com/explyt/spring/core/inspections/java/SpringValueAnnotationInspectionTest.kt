/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringValueAnnotationInspection
import com.explyt.spring.core.inspections.quickfix.ReplacementStringQuickFix
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import junit.framework.TestCase

class SpringValueAnnotationInspectionTest : ExplytInspectionJavaTestCase() {
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
