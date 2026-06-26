/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4BootstrapPackageInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4BootstrapPackageInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package org.springframework.boot; public interface BootstrapRegistry { }")
        myFixture.addClass("package org.springframework.boot.bootstrap; public interface BootstrapRegistry { }")
        myFixture.enableInspections(SpringBoot4BootstrapPackageInspection::class.java)
    }

    fun testLegacyBootstrapImportReported() {
        myFixture.configureByText(
            "MyConfig.java",
            """
            <warning descr="This type moved to org.springframework.boot.bootstrap in Spring Boot 4">import org.springframework.boot.BootstrapRegistry;</warning>
            
            public class MyConfig {
                BootstrapRegistry registry;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyConfig.java")
    }

    fun testNewBootstrapImportNotReported() {
        myFixture.configureByText(
            "MyConfig.java",
            """
            import org.springframework.boot.bootstrap.BootstrapRegistry;
            
            public class MyConfig {
                BootstrapRegistry registry;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyConfig.java")
    }

    fun testQuickFixUpdatesImport() {
        myFixture.configureByText(
            "MyConfig.java",
            """
            import org.springframework.boot.Bootstrap<caret>Registry;
            
            public class MyConfig {
                BootstrapRegistry registry;
            }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("bootstrap") || it.text.contains("Spring Boot 4") }
        requireNotNull(intention) { "Import migration fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "import should be migrated:\n$result",
            result.contains("import org.springframework.boot.bootstrap.BootstrapRegistry;")
        )
        assertFalse(
            "old import should be replaced:\n$result",
            result.contains("import org.springframework.boot.BootstrapRegistry;")
        )
    }
}
