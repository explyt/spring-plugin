/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringScheduledInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

class SpringScheduledInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringScheduledInspection::class.java)
    }

    fun testValidCron() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(cron="* * * * * *")
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValidFixedDelay() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedDelay=1234)
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValidFixedDelayString() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedDelayString="1")
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValidFixedRate() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRate=1234)
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValidFixedRateString() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString="1")
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testEmptyScheduled() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                <warning>@${SpringCoreClasses.SCHEDULED}()</warning>
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testInitDelayString() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(initialDelayString="123", fixedRate=1)
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testInitDelay() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(initialDelay=123, fixedRate=1)
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testInitValuesInvalid() {
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                <warning>@${SpringCoreClasses.SCHEDULED}(initialDelay=12, initialDelayString="123", fixedRate=1)</warning>
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValueFromConfig() {
        myFixture.addFileToProject("application.properties", "test.property=1")
        val propertyString = "\\\${test.property}"
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString="$propertyString")
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValueFromConfigInvalid() {
        myFixture.addFileToProject("application.properties", "test.property=1a")
        val propertyString = "\\\${test.property}"
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString=<warning>"$propertyString"</warning>)
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }

    fun testValueDefault() {
        val propertyString = "\\\${test.property: 10}"
        myFixture.configureByText(
            "SpringBean.kt",
            """
            @${SpringCoreClasses.COMPONENT}     
            class SpringBean {
                @${SpringCoreClasses.SCHEDULED}(fixedRateString=<warning>"$propertyString"</warning>)
                fun scheduledMethod() {}
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.kt")
    }
}
