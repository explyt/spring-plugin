/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4EntityScanPackageInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The realistic post-upgrade state: Spring Boot 4 relocated `@EntityScan`, so the legacy FQN no longer resolves.
 * Only the replacement is added to the fixture.
 */
class SpringBoot4EntityScanPackageUnresolvedImportTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            package org.springframework.boot.persistence.autoconfigure;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface EntityScan { String[] value() default {}; }
            """.trimIndent()
        )
        myFixture.enableInspections(SpringBoot4EntityScanPackageInspection::class.java)
    }

    fun testUnresolvedLegacyAnnotationReported() {
        assertReported(
            """
            import org.springframework.boot.autoconfigure.domain.EntityScan;
            
            @EntityScan("com.example.domain")
            public class AppConfig { }
            """
        )
    }

    fun testUnresolvedOnDemandImportReported() {
        assertReported(
            """
            import org.springframework.boot.autoconfigure.domain.*;
            
            @EntityScan("com.example.domain")
            public class AppConfig { }
            """
        )
    }

    fun testUnresolvedFullyQualifiedAnnotationReported() {
        assertReported(
            """
            @org.springframework.boot.autoconfigure.domain.EntityScan("com.example.domain")
            public class AppConfig { }
            """
        )
    }

    fun testMigratedAnnotationNotReported() {
        val messages = migrationMessages(
            """
            import org.springframework.boot.persistence.autoconfigure.EntityScan;
            
            @EntityScan("com.example.domain")
            public class AppConfig { }
            """
        )
        assertEmpty("already migrated code must stay clean: $messages", messages)
    }

    fun testUnrelatedUnresolvedAnnotationNotReported() {
        val messages = migrationMessages(
            """
            import com.example.EntityScan;
            
            @EntityScan("com.example.domain")
            public class AppConfig { }
            """
        )
        assertEmpty("an unrelated annotation must not be reported: $messages", messages)
    }

    private fun assertReported(code: String) {
        val messages = migrationMessages(code)
        assertTrue("expected '$MESSAGE', got: $messages", messages.contains(MESSAGE))
    }

    private fun migrationMessages(code: String): List<String> {
        myFixture.configureByText("AppConfig.java", code.trimIndent())
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("Spring Boot 4") }
    }

    private companion object {
        val MESSAGE: String = SpringCoreBundle.message("explyt.spring.inspection.boot4.entityscan")
    }
}
