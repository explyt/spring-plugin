/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4EntityScanPackageInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4EntityScanPackageInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        addEntityScanStubs()
        myFixture.enableInspections(SpringBoot4EntityScanPackageInspection::class.java)
    }

    private fun addEntityScanStubs() {
        myFixture.addClass(
            """
            package org.springframework.boot.autoconfigure.domain;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE)
            public @interface EntityScan { String[] value() default {}; }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.boot.persistence.autoconfigure;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.TYPE)
            public @interface EntityScan { String[] value() default {}; }
            """.trimIndent()
        )
    }

    fun testLegacyEntityScanImportReported() {
        myFixture.configureByText(
            "AppConfig.java",
            """
            <warning descr="@EntityScan moved to a new package in Spring Boot 4">import org.springframework.boot.autoconfigure.domain.EntityScan;</warning>
            
            @EntityScan("com.example.domain")
            public class AppConfig { }
            """.trimIndent()
        )
        myFixture.testHighlighting("AppConfig.java")
    }

    fun testNewEntityScanImportNotReported() {
        myFixture.configureByText(
            "AppConfig.java",
            """
            import org.springframework.boot.persistence.autoconfigure.EntityScan;
            
            @EntityScan("com.example.domain")
            public class AppConfig { }
            """.trimIndent()
        )
        myFixture.testHighlighting("AppConfig.java")
    }

    fun testQuickFixUpdatesImportPreservingAttributes() {
        myFixture.configureByText(
            "AppConfig.java",
            """
            import org.springframework.boot.autoconfigure.domain.Entity<caret>Scan;
            
            @EntityScan("com.example.domain")
            public class AppConfig { }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("EntityScan") || it.text.contains("Spring Boot 4") }
        requireNotNull(intention) { "Import migration fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "import should be migrated:\n$result",
            result.contains("import org.springframework.boot.persistence.autoconfigure.EntityScan;")
        )
        assertFalse(
            "old import should be replaced:\n$result",
            result.contains("import org.springframework.boot.autoconfigure.domain.EntityScan;")
        )
        // attribute usage must be preserved
        assertTrue("annotation usage must be preserved:\n$result", result.contains("@EntityScan(\"com.example.domain\")"))
    }
}
