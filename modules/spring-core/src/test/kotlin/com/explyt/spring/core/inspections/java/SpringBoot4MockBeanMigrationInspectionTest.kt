/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4MockBeanMigrationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The upper end of the deprecation window: on Spring Boot 3.5 the legacy annotations are still on the classpath (the
 * fixture stubs them accordingly) and both they and the replacements resolve, so the migration is reported as a weak
 * warning. The state after the Spring Boot 4 removal is covered by [SpringBoot4MockBeanMigrationUnresolvedImportTest].
 */
class SpringBoot4MockBeanMigrationInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_5_0
    )

    override fun setUp() {
        super.setUp()
        addAnnotationStubs()
        myFixture.enableInspections(SpringBoot4MockBeanMigrationInspection::class.java)
    }

    private fun addAnnotationStubs() {
        myFixture.addClass(
            """
            package org.springframework.boot.test.mock.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.TYPE})
            public @interface MockBean { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.boot.test.mock.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.TYPE})
            public @interface SpyBean { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.test.context.bean.override.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.TYPE})
            public @interface MockitoBean { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.test.context.bean.override.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.FIELD, ElementType.TYPE})
            public @interface MockitoSpyBean { }
            """.trimIndent()
        )
        myFixture.addClass("public class UserService { }")
    }

    fun testMockBeanReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.test.mock.mockito.MockBean;
            
            public class MyTest {
                <weak_warning descr="This annotation is deprecated since Spring Boot 3.4 and removed in Spring Boot 4. Use '@MockitoBean'">@MockBean</weak_warning>
                private UserService userService;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testSpyBeanReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.test.mock.mockito.SpyBean;
            
            public class MyTest {
                <weak_warning descr="This annotation is deprecated since Spring Boot 3.4 and removed in Spring Boot 4. Use '@MockitoSpyBean'">@SpyBean</weak_warning>
                private UserService userService;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testMockitoBeanNotReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.test.context.bean.override.mockito.MockitoBean;
            
            public class MyTest {
                @MockitoBean
                private UserService userService;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testMockBeanQuickFix() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.test.mock.mockito.MockBean;
            
            public class MyTest {
                @Mock<caret>Bean
                private UserService userService;
            }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("MockitoBean") }
        requireNotNull(intention) { "Replace-annotation quick-fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue("new annotation should be applied:\n$result", result.contains("@MockitoBean"))
        assertTrue(
            "new import should be added:\n$result",
            result.contains("import org.springframework.test.context.bean.override.mockito.MockitoBean;")
        )
        // The annotation use itself must be replaced; the now-unused old import is left for the IDE's
        // "unused import" inspection to clean up and is not the responsibility of this quick-fix.
        assertFalse("old annotation use should be removed:\n$result", result.contains("@MockBean"))
    }
}
