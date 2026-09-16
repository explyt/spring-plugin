/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBeanSystemEnvironmentAccessInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBeanSystemEnvironmentAccessInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanSystemEnvironmentAccessInspection::class.java)
    }

    fun testSystemGetenvInBean() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                public String read() {
                    return <warning>System.getenv("APP_HOME")</warning>;
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testSystemGetenvInStaticMain() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                public static void main(String[] args) {
                    System.out.println(System.getenv("APP_HOME"));
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testSystemGetenvInPlainClass() {
        myFixture.configureByText(
            "PlainService.java",
            """
            public class PlainService {
                public String read() {
                    return System.getenv("APP_HOME");
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("PlainService.java")
    }

    fun testSystemGetenvInStaticInitializer() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                private static final String HOME;

                static {
                    HOME = System.getenv("APP_HOME");
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testSystemGetPropertyForJvmKey() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                public String read() {
                    return System.getProperty("java.io.tmpdir");
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testSystemGetenvWithDynamicKey() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                public String read(String name) {
                    return System.getenv(name);
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testQuickFixReplacesWithValueDependency() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                public String read() {
                    return <caret>System.getenv("APP_HOME");
                }
            }
            """.trimIndent()
        )
        myFixture.launchAction(intention("explyt.spring.inspection.system.env.fix.value"))
        val text = myFixture.file.text
        assertTrue(text, text.contains("appHome"))
        assertFalse(text, text.contains("System.getenv"))
    }

    fun testQuickFixReplacesWithEnvironmentUsingExistingField() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                private org.springframework.core.env.Environment environment;

                public String read() {
                    return <caret>System.getenv("APP_HOME");
                }
            }
            """.trimIndent()
        )
        myFixture.launchAction(intention("explyt.spring.inspection.system.env.fix.environment"))
        myFixture.checkResult(
            """
            @org.springframework.stereotype.Component
            public class SpringBean {
                private org.springframework.core.env.Environment environment;

                public String read() {
                    return environment.getProperty("APP_HOME");
                }
            }
            """.trimIndent(), true
        )
    }

    private fun intention(messageKey: String) = myFixture.getAvailableIntentions()
        .first { it.text == SpringCoreBundle.message(messageKey, "APP_HOME") }
}
