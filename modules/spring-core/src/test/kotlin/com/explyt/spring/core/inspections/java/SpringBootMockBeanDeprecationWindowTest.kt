/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4MockBeanMigrationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity

/**
 * The deprecation window at its lower bound: Spring Boot 3.4 ships the replacements (Spring Framework 6.2) and
 * deprecates `@MockBean`/`@SpyBean` for removal in 4.0, so both the legacy and the replacement annotations are on the
 * classpath and the migration can already be applied.
 */
class SpringBootMockBeanDeprecationWindowTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_4_0
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

    fun testDeprecatedMockBeanReported() {
        val problem = singleProblem(
            """
            import org.springframework.boot.test.mock.mockito.MockBean;
            
            public class MyTest {
                @MockBean
                private UserService userService;
            }
            """
        )
        assertEquals(deprecatedMessage("MockitoBean"), problem.description)
    }

    fun testDeprecatedSpyBeanReported() {
        val problem = singleProblem(
            """
            import org.springframework.boot.test.mock.mockito.SpyBean;
            
            public class MyTest {
                @SpyBean
                private UserService userService;
            }
            """
        )
        assertEquals(deprecatedMessage("MockitoSpyBean"), problem.description)
    }

    /**
     * The IDE already highlights the deprecation itself while the annotation is on the classpath; what this
     * inspection adds is the replacement name and the quick-fix, so it must not paint a second full-strength warning.
     */
    fun testDeprecatedAnnotationReportedAsWeakWarning() {
        val problem = singleProblem(
            """
            import org.springframework.boot.test.mock.mockito.MockBean;
            
            public class MyTest {
                @MockBean
                private UserService userService;
            }
            """
        )
        assertEquals(HighlightSeverity.WEAK_WARNING, problem.severity)
    }

    fun testMigratedAnnotationNotReported() {
        val problems = migrationProblems(
            """
            import org.springframework.test.context.bean.override.mockito.MockitoBean;
            
            public class MyTest {
                @MockitoBean
                private UserService userService;
            }
            """
        )
        assertEmpty("already migrated code must stay clean: ${problems.map { it.description }}", problems)
    }

    fun testQuickFixReplacesDeprecatedAnnotation() {
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
        val fixName = SpringCoreBundle.message("explyt.spring.inspection.replace.annotation.fix", "MockitoBean")
        val intention = myFixture.availableIntentions.firstOrNull { it.text == fixName }
        requireNotNull(intention) {
            "'$fixName' not found; available: " + myFixture.availableIntentions.map { it.text }
        }
        myFixture.launchAction(intention)
        assertEquals(
            """
            import org.springframework.test.context.bean.override.mockito.MockitoBean;
            
            public class MyTest {
                @MockitoBean
                private UserService userService;
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
        myFixture.configureByText("MyTest.java", code.trimIndent())
        return myFixture.doHighlighting().filter { it.description?.contains("Use '@Mockito") == true }
    }

    private companion object {
        fun deprecatedMessage(replacement: String): String =
            SpringCoreBundle.message("explyt.spring.inspection.boot4.mockbean.deprecated", replacement)
    }
}
