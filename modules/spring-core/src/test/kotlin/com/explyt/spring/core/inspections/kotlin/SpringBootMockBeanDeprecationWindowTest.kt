/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4MockBeanMigrationInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity

/**
 * Kotlin twin of [com.explyt.spring.core.inspections.java.SpringBootMockBeanDeprecationWindowTest].
 *
 * The stubs repeat the real `@Target({TYPE, FIELD})` of `@MockBean`/`@SpyBean`: adding `PROPERTY` would make Kotlin
 * bind the annotation to the property instead of the backing field, which is not how the real annotation behaves.
 */
class SpringBootMockBeanDeprecationWindowTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_4_0
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

    fun testDeprecatedMockBeanReported() {
        val problem = singleProblem(
            """
            import org.springframework.boot.test.mock.mockito.MockBean
            
            class MyTest {
                @MockBean
                private lateinit var userService: UserService
            }
            """
        )
        assertEquals(deprecatedMessage("MockitoBean"), problem.description)
        assertEquals(HighlightSeverity.WEAK_WARNING, problem.severity)
    }

    fun testDeprecatedSpyBeanReported() {
        val problem = singleProblem(
            """
            import org.springframework.boot.test.mock.mockito.SpyBean
            
            class MyTest {
                @SpyBean
                private lateinit var userService: UserService
            }
            """
        )
        assertEquals(deprecatedMessage("MockitoSpyBean"), problem.description)
        assertEquals(HighlightSeverity.WEAK_WARNING, problem.severity)
    }

    fun testMigratedAnnotationNotReported() {
        val problems = migrationProblems(
            """
            import org.springframework.test.context.bean.override.mockito.MockitoBean
            
            class MyTest {
                @MockitoBean
                private lateinit var userService: UserService
            }
            """
        )
        assertEmpty("already migrated code must stay clean: ${problems.map { it.description }}", problems)
    }

    fun testQuickFixReplacesDeprecatedAnnotation() {
        myFixture.configureByText(
            "MyTest.kt",
            """
            import org.springframework.boot.test.mock.mockito.MockBean
            
            class MyTest {
                @Mock<caret>Bean
                private lateinit var userService: UserService
            }
            """.trimIndent()
        )
        val fixName = SpringCoreBundle.message("explyt.spring.inspection.replace.annotation.fix", "MockitoBean")
        val intention = myFixture.availableIntentions.firstOrNull { it.text == fixName }
        requireNotNull(intention) {
            "'$fixName' not found; available: " + myFixture.availableIntentions.map { it.text }
        }
        myFixture.launchAction(intention)
        assertEquals(
            """
            import org.springframework.test.context.bean.override.mockito.MockitoBean
            
            class MyTest {
                @MockitoBean
                private lateinit var userService: UserService
            }
            """.trimIndent(),
            myFixture.file.text
        )
    }

    private fun singleProblem(code: String): HighlightInfo {
        val problems = migrationProblems(code)
        assertSize(1, problems)
        return problems.single()
    }

    private fun migrationProblems(code: String): List<HighlightInfo> {
        myFixture.configureByText("MyTest.kt", code.trimIndent())
        return myFixture.doHighlighting().filter { it.description?.contains("Use '@Mockito") == true }
    }

    private companion object {
        fun deprecatedMessage(replacement: String): String =
            SpringCoreBundle.message("explyt.spring.inspection.boot4.mockbean.deprecated", replacement)
    }
}
