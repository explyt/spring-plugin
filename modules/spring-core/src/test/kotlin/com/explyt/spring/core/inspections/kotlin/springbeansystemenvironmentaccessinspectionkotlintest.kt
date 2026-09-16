/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBeanSystemEnvironmentAccessInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

class SpringBeanSystemEnvironmentAccessInspectionKotlinTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.kotlin_1_9_22)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanSystemEnvironmentAccessInspection::class.java)
    }

    fun testSystemGetenvInComponent() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @org.springframework.stereotype.Component
            class SpringBean {
                fun read(): String {
                    return <warning>System.getenv("APP_HOME")</warning>
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testSystemGetenvInCompanionMain() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @org.springframework.stereotype.Component
            class SpringBean {
                companion object {
                    @JvmStatic
                    fun main(args: Array<String>) {
                        println(System.getenv("APP_HOME"))
                    }
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testSystemGetenvInPlainClass() {
        myFixture.configureByText(
            "PlainService.kt",
            """
            class PlainService {
                fun read(): String? {
                    return System.getenv("APP_HOME")
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("PlainService.kt")
    }

    fun testSystemGetPropertyForJvmKey() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @org.springframework.stereotype.Component
            class SpringBean {
                fun read(): String? {
                    return System.getProperty("java.io.tmpdir")
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testQuickFixReplacesWithValueProperty() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @org.springframework.stereotype.Component
            class SpringBean {
                fun read(): String? {
                    return <caret>System.getenv("APP_HOME")
                }
            }
            """.trimIndent()
        )
        myFixture.launchAction(intention("explyt.spring.inspection.system.env.fix.value"))
        val text = myFixture.file.text
        assertTrue(text, text.contains("appHome"))
        assertFalse(text, text.contains("System.getenv"))
    }

    fun testQuickFixReplacesWithEnvironmentUsingExistingParameter() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @org.springframework.stereotype.Component
            class SpringBean(private val environment: org.springframework.core.env.Environment) {
                fun read(): String? {
                    return <caret>System.getenv("APP_HOME")
                }
            }
            """.trimIndent()
        )
        myFixture.launchAction(intention("explyt.spring.inspection.system.env.fix.environment"))
        myFixture.checkResult(
            """
            @org.springframework.stereotype.Component
            class SpringBean(private val environment: org.springframework.core.env.Environment) {
                fun read(): String? {
                    return environment.getProperty("APP_HOME")
                }
            }
            """.trimIndent(), true
        )
    }

    private fun intention(messageKey: String) = myFixture.getAvailableIntentions()
        .first { it.text == SpringCoreBundle.message(messageKey, "APP_HOME") }
}
