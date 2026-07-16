/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot3ActuatorHttpExchangesInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot3ActuatorHttpExchangesInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBoot3ActuatorHttpExchangesInspection::class.java)
    }

    fun testLegacyHttpTraceEndpointReported() {
        myFixture.configureByText(
            "application.properties",
            """
            management.endpoints.web.exposure.include=<warning descr="The 'httptrace' actuator endpoint was renamed to 'httpexchanges' in Spring Boot 3">health,httptrace</warning>
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testNonLegacyExposureNotReported() {
        myFixture.configureByText(
            "application.properties",
            """
            management.endpoints.web.exposure.include=health,httpexchanges,info
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testQuickFixRenamesEndpoint() {
        myFixture.configureByText(
            "application.properties",
            """
            management.endpoints.web.exposure.include=health,http<caret>trace,info
            """.trimIndent()
        )
        val fixName = SpringCoreBundle.message("explyt.spring.inspection.boot3.actuator.httptrace.fix")
        val intention = myFixture.findSingleIntention(fixName)
        myFixture.launchAction(intention)
        myFixture.checkResult(
            """
            management.endpoints.web.exposure.include=health,httpexchanges,info
            """.trimIndent()
        )
    }
}
