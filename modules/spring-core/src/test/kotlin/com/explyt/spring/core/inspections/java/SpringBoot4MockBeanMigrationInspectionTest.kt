/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4MockBeanMigrationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4MockBeanMigrationInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
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
                <warning descr="This annotation was removed in Spring Boot 4. Use '@MockitoBean'">@MockBean</warning>
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
                <warning descr="This annotation was removed in Spring Boot 4. Use '@MockitoSpyBean'">@SpyBean</warning>
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
