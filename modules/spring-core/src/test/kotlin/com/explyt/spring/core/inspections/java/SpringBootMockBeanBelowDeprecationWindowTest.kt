/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4MockBeanMigrationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo

/**
 * Negative control for a project that has not entered the deprecation window: Spring Boot 3.3 runs on Spring Framework
 * 6.1, where `@MockBean`/`@SpyBean` are not deprecated and their replacements do not exist yet. Nothing may be
 * reported - the annotations are the correct ones for that version.
 *
 * The replacement stubs are added deliberately, even though a real 3.3 project cannot have them: they neutralise the
 * replacement-availability gate, so this test can only stay green because of the Spring Boot version floor. Without
 * that floor it fails, which is what makes the control meaningful.
 */
class SpringBootMockBeanBelowDeprecationWindowTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_3_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            package org.springframework.boot.test.mock.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.FIELD})
            public @interface MockBean { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.boot.test.mock.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.FIELD})
            public @interface SpyBean { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.test.context.bean.override.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.FIELD})
            public @interface MockitoBean { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.test.context.bean.override.mockito;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.FIELD})
            public @interface MockitoSpyBean { }
            """.trimIndent()
        )
        myFixture.addClass("public class UserService { }")
        myFixture.enableInspections(SpringBoot4MockBeanMigrationInspection::class.java)
    }

    fun testMockBeanNotReportedBelow34() {
        val problems = migrationProblems(
            """
            import org.springframework.boot.test.mock.mockito.MockBean;
            
            public class MyTest {
                @MockBean
                private UserService userService;
            }
            """
        )
        assertEmpty("Spring Boot 3.3 must not be nagged: ${problems.map { it.description }}", problems)
    }

    fun testSpyBeanNotReportedBelow34() {
        val problems = migrationProblems(
            """
            import org.springframework.boot.test.mock.mockito.SpyBean;
            
            public class MyTest {
                @SpyBean
                private UserService userService;
            }
            """
        )
        assertEmpty("Spring Boot 3.3 must not be nagged: ${problems.map { it.description }}", problems)
    }

    private fun migrationProblems(code: String): List<HighlightInfo> {
        myFixture.configureByText("MyTest.java", code.trimIndent())
        return myFixture.doHighlighting().filter { it.description?.contains("Use '@Mockito") == true }
    }
}
