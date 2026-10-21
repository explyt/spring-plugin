/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4BootstrapPackageInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Kotlin twin of the Java test: covers the `KtFile` branch of the shared legacy-symbol lookup, including the star
 * import and the import alias, which have no Java counterpart.
 *
 * Highlighting is asserted by filtering the reported problems instead of comparing against inline markup: an
 * unresolved type always produces "Cannot resolve" errors, which markup comparison would demand be spelled out.
 */
class SpringBoot4BootstrapPackageUnresolvedImportTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package org.springframework.boot.bootstrap; public interface BootstrapRegistry { }")
        myFixture.enableInspections(SpringBoot4BootstrapPackageInspection::class.java)
    }

    fun testUnresolvedLegacyPropertyTypeReported() {
        assertReported(
            """
            import org.springframework.boot.BootstrapRegistry
            
            class MyConfig {
                lateinit var registry: BootstrapRegistry
            }
            """
        )
    }

    fun testUnresolvedLegacyParameterTypeReported() {
        assertReported(
            """
            import org.springframework.boot.BootstrapRegistry
            
            class MyConfig {
                fun run(registry: BootstrapRegistry) { }
            }
            """
        )
    }

    fun testUnresolvedStarImportReported() {
        assertReported(
            """
            import org.springframework.boot.*
            
            class MyConfig {
                lateinit var registry: BootstrapRegistry
            }
            """
        )
    }

    fun testUnresolvedImportAliasReported() {
        assertReported(
            """
            import org.springframework.boot.BootstrapRegistry as BR
            
            class MyConfig {
                lateinit var registry: BR
            }
            """
        )
    }

    fun testUnresolvedFullyQualifiedTypeReported() {
        assertReported(
            """
            class MyConfig {
                lateinit var registry: org.springframework.boot.BootstrapRegistry
            }
            """
        )
    }

    fun testMigratedTypeNotReported() {
        val messages = migrationMessages(
            """
            import org.springframework.boot.bootstrap.BootstrapRegistry
            
            class MyConfig {
                lateinit var registry: BootstrapRegistry
            }
            """
        )
        assertEmpty("already migrated code must stay clean: $messages", messages)
    }

    fun testUnrelatedUnresolvedTypeNotReported() {
        val messages = migrationMessages(
            """
            import com.example.BootstrapRegistry
            
            class MyConfig {
                lateinit var registry: BootstrapRegistry
            }
            """
        )
        assertEmpty("an unrelated type must not be reported: $messages", messages)
    }

    private fun assertReported(code: String) {
        val messages = migrationMessages(code)
        assertTrue("expected '$MESSAGE', got: $messages", messages.contains(MESSAGE))
    }

    private fun migrationMessages(code: String): List<String> {
        myFixture.configureByText("MyConfig.kt", code.trimIndent())
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("Spring Boot 4") }
    }

    private companion object {
        val MESSAGE: String = SpringCoreBundle.message("explyt.spring.inspection.boot4.bootstrap")
    }
}
