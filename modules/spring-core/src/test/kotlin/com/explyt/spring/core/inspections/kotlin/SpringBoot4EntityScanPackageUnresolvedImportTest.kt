/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4EntityScanPackageInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Kotlin twin of the Java test: covers the `KtFile` branch of the shared legacy-symbol lookup, including the star
 * import and the import alias, which have no Java counterpart.
 */
class SpringBoot4EntityScanPackageUnresolvedImportTest : ExplytInspectionKotlinTestCase() {
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
            import org.springframework.boot.autoconfigure.domain.EntityScan
            
            @EntityScan("com.example.domain")
            class AppConfig
            """
        )
    }

    fun testUnresolvedStarImportReported() {
        assertReported(
            """
            import org.springframework.boot.autoconfigure.domain.*
            
            @EntityScan("com.example.domain")
            class AppConfig
            """
        )
    }

    fun testUnresolvedImportAliasReported() {
        assertReported(
            """
            import org.springframework.boot.autoconfigure.domain.EntityScan as LegacyEntityScan
            
            @LegacyEntityScan("com.example.domain")
            class AppConfig
            """
        )
    }

    fun testUnresolvedFullyQualifiedAnnotationReported() {
        assertReported(
            """
            @org.springframework.boot.autoconfigure.domain.EntityScan("com.example.domain")
            class AppConfig
            """
        )
    }

    fun testMigratedAnnotationNotReported() {
        val messages = migrationMessages(
            """
            import org.springframework.boot.persistence.autoconfigure.EntityScan
            
            @EntityScan("com.example.domain")
            class AppConfig
            """
        )
        assertEmpty("already migrated code must stay clean: $messages", messages)
    }

    fun testUnrelatedUnresolvedAnnotationNotReported() {
        val messages = migrationMessages(
            """
            import com.example.EntityScan
            
            @EntityScan("com.example.domain")
            class AppConfig
            """
        )
        assertEmpty("an unrelated annotation must not be reported: $messages", messages)
    }

    /**
     * `RewriteAnnotationQuickFix` rejects a `LightElement` owner, and a Kotlin class reaches it as `KtLightClass`,
     * so the reported problem carries no applicable fix. The test pins the current behaviour: if a Kotlin-aware fix
     * is added later this assertion fails and has to be replaced by a result assertion.
     *
     * The fix is identified by the "Annotate" wording of `AddAnnotationPsiFix`; matching on the annotation name
     * instead would also catch the platform's own `Import class` / `Create annotation` intentions.
     */
    fun testQuickFixUnavailableOnKotlin() {
        myFixture.configureByText(
            "AppConfig.kt",
            """
            import org.springframework.boot.autoconfigure.domain.EntityScan
            
            @Entity<caret>Scan("com.example.domain")
            class AppConfig
            """.trimIndent()
        )
        val migrationIntentions = myFixture.availableIntentions
            .map { it.text }
            .filter { it.contains("Annotate") }
        assertEmpty(
            "unexpected migration fix on Kotlin - the LightElement limitation may be gone: $migrationIntentions",
            migrationIntentions
        )
    }

    private fun assertReported(code: String) {
        val messages = migrationMessages(code)
        assertTrue("expected '$MESSAGE', got: $messages", messages.contains(MESSAGE))
    }

    private fun migrationMessages(code: String): List<String> {
        myFixture.configureByText("AppConfig.kt", code.trimIndent())
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("Spring Boot 4") }
    }

    private companion object {
        val MESSAGE: String = SpringCoreBundle.message("explyt.spring.inspection.boot4.entityscan")
    }
}
