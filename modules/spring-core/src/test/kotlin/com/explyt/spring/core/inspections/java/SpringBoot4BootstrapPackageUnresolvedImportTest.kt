/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4BootstrapPackageInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The realistic post-upgrade state: Spring Boot 4 relocated the bootstrap types, so the legacy FQNs no longer
 * resolve. Only the replacement is added to the fixture.
 *
 * Highlighting is asserted by filtering the reported problems instead of comparing against inline markup: an
 * unresolved type always produces "Cannot resolve symbol" errors, which markup comparison would demand be spelled
 * out and which are irrelevant here.
 */
class SpringBoot4BootstrapPackageUnresolvedImportTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package org.springframework.boot.bootstrap; public interface BootstrapRegistry { }")
        myFixture.enableInspections(SpringBoot4BootstrapPackageInspection::class.java)
    }

    fun testUnresolvedLegacyFieldTypeReported() {
        assertReported(
            """
            import org.springframework.boot.BootstrapRegistry;
            
            public class MyConfig {
                BootstrapRegistry registry;
            }
            """
        )
    }

    fun testUnresolvedLegacyParameterTypeReported() {
        assertReported(
            """
            import org.springframework.boot.BootstrapRegistry;
            
            public class MyConfig {
                void run(BootstrapRegistry registry) { }
            }
            """
        )
    }

    fun testUnresolvedOnDemandImportReported() {
        assertReported(
            """
            import org.springframework.boot.*;
            
            public class MyConfig {
                BootstrapRegistry registry;
            }
            """
        )
    }

    fun testUnresolvedFullyQualifiedTypeReported() {
        assertReported(
            """
            public class MyConfig {
                org.springframework.boot.BootstrapRegistry registry;
            }
            """
        )
    }

    fun testMigratedTypeNotReported() {
        val messages = migrationMessages(
            """
            import org.springframework.boot.bootstrap.BootstrapRegistry;
            
            public class MyConfig {
                BootstrapRegistry registry;
            }
            """
        )
        assertEmpty("already migrated code must stay clean: $messages", messages)
    }

    fun testUnrelatedUnresolvedTypeNotReported() {
        val messages = migrationMessages(
            """
            import com.example.BootstrapRegistry;
            
            public class MyConfig {
                BootstrapRegistry registry;
            }
            """
        )
        assertEmpty("an unrelated type must not be reported: $messages", messages)
    }

    fun testQuickFixMigratesUnresolvedImport() {
        myFixture.configureByText(
            "MyConfig.java",
            """
            import org.springframework.boot.BootstrapRegistry;
            
            public class MyConfig {
                Bootstrap<caret>Registry registry;
            }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions.firstOrNull { it.text.contains("Spring Boot 4") }
        requireNotNull(intention) {
            "Import migration fix not found; available: " + myFixture.availableIntentions.map { it.text }
        }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "import should be migrated:\n$result",
            result.contains("import org.springframework.boot.bootstrap.BootstrapRegistry;")
        )
    }

    private fun assertReported(code: String) {
        val messages = migrationMessages(code)
        assertTrue("expected '$MESSAGE', got: $messages", messages.contains(MESSAGE))
    }

    private fun migrationMessages(code: String): List<String> {
        myFixture.configureByText("MyConfig.java", code.trimIndent())
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("Spring Boot 4") }
    }

    private companion object {
        val MESSAGE: String = SpringCoreBundle.message("explyt.spring.inspection.boot4.bootstrap")
    }
}
