/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot3PropertyMigrationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot3PropertyMigrationInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBoot3PropertyMigrationInspection::class.java)
    }

    fun testRenamedExactKeyReported() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="This property was renamed in Spring Boot 3. Use 'server.max-http-request-header-size'">server.max-http-header-size</warning>=8KB
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testRenamedPrefixKeyReported() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="This property was renamed in Spring Boot 3. Use 'spring.data.redis.host'">spring.redis.host</warning>=localhost
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testRemovedKeyReported() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="This property is no longer supported in Spring Boot 3">spring.session.store-type</warning>=redis
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testNonMigratedKeyNotReported() {
        myFixture.configureByText(
            "application.properties",
            """
            server.port=8080
            spring.application.name=demo
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testRenameQuickFix() {
        myFixture.configureByText(
            "application.properties",
            """
            server.max-http-header-size=8KB
            """.trimIndent()
        )
        val fixName = SpringCoreBundle.message(
            "explyt.spring.inspection.properties.quick.fix.replacement",
            "server.max-http-request-header-size"
        )
        val intention = myFixture.findSingleIntention(fixName)
        myFixture.launchAction(intention)
        myFixture.checkResult(
            """
            server.max-http-request-header-size=8KB
            """.trimIndent()
        )
    }

    fun testRemoveQuickFix() {
        myFixture.configureByText(
            "application.properties",
            """
            spring.session.store-type=redis
            """.trimIndent()
        )
        // For .properties files the platform's built-in "Remove property" fix is used.
        val intention = myFixture.findSingleIntention("Remove property")
        myFixture.launchAction(intention)
        myFixture.checkResult("")
    }
}
