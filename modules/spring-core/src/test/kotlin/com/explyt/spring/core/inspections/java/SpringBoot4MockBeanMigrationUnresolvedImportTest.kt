/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4MockBeanMigrationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The realistic post-upgrade state: Spring Boot 4 removed `org.springframework.boot.test.mock.mockito`, so the legacy
 * annotations do not resolve and the fixture deliberately provides only the replacements.
 *
 * Highlighting is asserted by filtering [com.intellij.codeInsight.daemon.impl.HighlightInfo]s instead of comparing
 * against inline markup: an unresolved annotation always produces "Cannot resolve symbol" errors, which markup
 * comparison would demand to be spelled out and which are irrelevant to this inspection.
 */
class SpringBoot4MockBeanMigrationUnresolvedImportTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
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
        myFixture.enableInspections(SpringBoot4MockBeanMigrationInspection::class.java)
    }

    fun testUnresolvedMockBeanReported() {
        assertMigrationReported(
            """
            import org.springframework.boot.test.mock.mockito.MockBean;
            
            public class MyTest {
                @MockBean
                private UserService userService;
            }
            """,
            MOCKITO_BEAN_MESSAGE
        )
    }

    fun testUnresolvedSpyBeanReported() {
        assertMigrationReported(
            """
            import org.springframework.boot.test.mock.mockito.SpyBean;
            
            public class MyTest {
                @SpyBean
                private UserService userService;
            }
            """,
            MOCKITO_SPY_BEAN_MESSAGE
        )
    }

    fun testUnresolvedOnDemandImportReported() {
        assertMigrationReported(
            """
            import org.springframework.boot.test.mock.mockito.*;
            
            public class MyTest {
                @MockBean
                private UserService userService;
            }
            """,
            MOCKITO_BEAN_MESSAGE
        )
    }

    fun testUnresolvedFullyQualifiedReported() {
        assertMigrationReported(
            """
            public class MyTest {
                @org.springframework.boot.test.mock.mockito.MockBean
                private UserService userService;
            }
            """,
            MOCKITO_BEAN_MESSAGE
        )
    }

    fun testUnresolvedAnnotationOnClassReported() {
        assertMigrationReported(
            """
            import org.springframework.boot.test.mock.mockito.MockBean;
            
            @MockBean
            public class MyTest {
            }
            """,
            MOCKITO_BEAN_MESSAGE
        )
    }

    fun testUnrelatedUnresolvedAnnotationNotReported() {
        val messages = migrationMessages(
            """
            import com.example.MockBean;
            
            public class MyTest {
                @MockBean
                private UserService userService;
            }
            """
        )
        assertEmpty("an unrelated annotation must not be reported: $messages", messages)
    }

    fun testQuickFixReplacesUnresolvedAnnotation() {
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
        val result = myFixture.file.text
        assertTrue("annotation should be replaced:\n$result", result.contains("@MockitoBean"))
    }

    private fun assertMigrationReported(code: String, expectedMessage: String) {
        val messages = migrationMessages(code)
        assertTrue("expected '$expectedMessage', got: $messages", messages.contains(expectedMessage))
    }

    private fun migrationMessages(code: String): List<String> {
        myFixture.configureByText("MyTest.java", code.trimIndent())
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("Spring Boot 4") }
    }

    private companion object {
        val MOCKITO_BEAN_MESSAGE: String =
            SpringCoreBundle.message("explyt.spring.inspection.boot4.mockbean", "MockitoBean")
        val MOCKITO_SPY_BEAN_MESSAGE: String =
            SpringCoreBundle.message("explyt.spring.inspection.boot4.mockbean", "MockitoSpyBean")
    }
}
