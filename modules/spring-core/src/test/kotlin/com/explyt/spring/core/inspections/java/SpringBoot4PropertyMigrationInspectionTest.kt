/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4PropertyMigrationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4PropertyMigrationInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBoot4PropertyMigrationInspection::class.java)
    }

    fun testRenamedExactKeyReported() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="This property was renamed in Spring Boot 4. Use 'spring.persistence.exceptiontranslation.enabled'">spring.dao.exceptiontranslation.enabled</warning>=true
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testRenamedPrefixKeyReported() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="This property was renamed in Spring Boot 4. Use 'spring.jackson.json.read.allow-trailing-tokens'">spring.jackson.read.allow-trailing-tokens</warning>=true
            """.trimIndent()
        )
        myFixture.testHighlighting("application.properties")
    }

    fun testSessionRedisPrefixRenamed() {
        myFixture.configureByText(
            "application.properties",
            """
            <warning descr="This property was renamed in Spring Boot 4. Use 'spring.session.data.redis.namespace'">spring.session.redis.namespace</warning>=spring:session
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
            spring.dao.exceptiontranslation.enabled=true
            """.trimIndent()
        )
        val fixName = SpringCoreBundle.message(
            "explyt.spring.inspection.properties.quick.fix.replacement",
            "spring.persistence.exceptiontranslation.enabled"
        )
        val intention = myFixture.findSingleIntention(fixName)
        myFixture.launchAction(intention)
        myFixture.checkResult(
            """
            spring.persistence.exceptiontranslation.enabled=true
            """.trimIndent()
        )
    }
}
