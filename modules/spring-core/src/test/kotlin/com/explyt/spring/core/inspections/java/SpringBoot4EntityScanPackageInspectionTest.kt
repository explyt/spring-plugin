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
        myFixture.addClass(
            """
            package org.springframework.boot.autoconfigure.domain;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface EntityScan { String[] value() default {}; }
            """.trimIndent()
        )
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

    fun testLegacyEntityScanAnnotationReported() {
        myFixture.configureByText(
            "AppConfig.java",
            """
            import org.springframework.boot.autoconfigure.domain.EntityScan;
            
            <warning descr="@EntityScan moved to org.springframework.boot.persistence.autoconfigure in Spring Boot 4">@EntityScan("com.example.domain")</warning>
            public class AppConfig { }
            """.trimIndent()
        )
        myFixture.testHighlighting("AppConfig.java")
    }

    fun testNewEntityScanNotReported() {
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

    fun testQuickFixReplacesAnnotationPreservingAttributes() {
        myFixture.configureByText(
            "AppConfig.java",
            """
            import org.springframework.boot.autoconfigure.domain.EntityScan;
            
            @Entity<caret>Scan("com.example.domain")
            public class AppConfig { }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("EntityScan") }
        requireNotNull(intention) { "EntityScan migration fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "new EntityScan import should be present:\n$result",
            result.contains("org.springframework.boot.persistence.autoconfigure.EntityScan")
        )
        assertTrue("attribute should be preserved:\n$result", result.contains("\"com.example.domain\""))
        assertFalse(
            "old annotation FQN should be gone from usage:\n$result",
            result.contains("org.springframework.boot.autoconfigure.domain.EntityScan")
        )
    }
}
