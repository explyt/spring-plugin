/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringBoot4JacksonAnnotationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The realistic post-upgrade state: Spring Boot 4 renamed the Jackson annotations, so the legacy ones no longer
 * resolve. Only the replacements are added to the fixture.
 */
class SpringBoot4JacksonAnnotationUnresolvedImportTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            package org.springframework.boot.jackson;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface JacksonComponent { Class<?>[] value() default {}; }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.boot.jackson;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface JacksonMixin { Class<?>[] value() default {}; }
            """.trimIndent()
        )
        myFixture.enableInspections(SpringBoot4JacksonAnnotationInspection::class.java)
    }

    fun testUnresolvedJsonComponentReported() {
        assertReported(
            """
            import org.springframework.boot.jackson.JsonComponent;
            
            @JsonComponent
            public class MyComponent { }
            """,
            componentMessage()
        )
    }

    fun testUnresolvedJsonMixinReported() {
        assertReported(
            """
            import org.springframework.boot.jackson.JsonMixin;
            
            @JsonMixin(String.class)
            public class MyComponent { }
            """,
            mixinMessage()
        )
    }

    fun testUnresolvedOnDemandImportReported() {
        assertReported(
            """
            import org.springframework.boot.jackson.*;
            
            @JsonComponent
            public class MyComponent { }
            """,
            componentMessage()
        )
    }

    fun testUnresolvedFullyQualifiedAnnotationReported() {
        assertReported(
            """
            @org.springframework.boot.jackson.JsonComponent
            public class MyComponent { }
            """,
            componentMessage()
        )
    }

    fun testMigratedAnnotationNotReported() {
        val messages = migrationMessages(
            """
            import org.springframework.boot.jackson.JacksonComponent;
            
            @JacksonComponent
            public class MyComponent { }
            """
        )
        assertEmpty("already migrated code must stay clean: $messages", messages)
    }

    fun testUnrelatedUnresolvedAnnotationNotReported() {
        val messages = migrationMessages(
            """
            import com.example.JsonComponent;
            
            @JsonComponent
            public class MyComponent { }
            """
        )
        assertEmpty("an unrelated annotation must not be reported: $messages", messages)
    }

    fun testQuickFixReplacesUnresolvedJsonComponent() {
        myFixture.configureByText(
            "MyComponent.java",
            """
            import org.springframework.boot.jackson.JsonComponent;

            @Json<caret>Component(String.class)
            public class MyComponent { }
            """.trimIndent()
        )
        val fixName = SpringCoreBundle.message("explyt.spring.inspection.replace.annotation.fix", "JacksonComponent")
        val intention = myFixture.availableIntentions.firstOrNull { it.text == fixName }
        requireNotNull(intention) {
            "JacksonComponent migration fix not found; available: " + myFixture.availableIntentions.map { it.text }
        }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "new annotation should be applied:\n$result",
            result.contains("@JacksonComponent") || result.contains("@org.springframework.boot.jackson.JacksonComponent")
        )
        assertTrue("attribute should be preserved:\n$result", result.contains("String.class"))
        assertFalse("old annotation should be replaced:\n$result", result.contains("@JsonComponent"))
    }

    fun testQuickFixReplacesUnresolvedJsonMixin() {
        myFixture.configureByText(
            "MyMixin.java",
            """
            import org.springframework.boot.jackson.JsonMixin;

            @Json<caret>Mixin(String.class)
            public class MyMixin { }
            """.trimIndent()
        )
        val fixName = SpringCoreBundle.message("explyt.spring.inspection.replace.annotation.fix", "JacksonMixin")
        val intention = myFixture.availableIntentions.firstOrNull { it.text == fixName }
        requireNotNull(intention) {
            "JacksonMixin migration fix not found; available: " + myFixture.availableIntentions.map { it.text }
        }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "new annotation should be applied:\n$result",
            result.contains("@JacksonMixin") || result.contains("@org.springframework.boot.jackson.JacksonMixin")
        )
        assertTrue("attribute should be preserved:\n$result", result.contains("String.class"))
        assertFalse("old annotation should be replaced:\n$result", result.contains("@JsonMixin"))
    }

    private fun assertReported(code: String, expectedMessage: String) {
        val messages = migrationMessages(code)
        assertTrue("expected '$expectedMessage', got: $messages", messages.contains(expectedMessage))
    }

    private fun migrationMessages(code: String): List<String> {
        myFixture.configureByText("MyComponent.java", code.trimIndent())
        return myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("Spring Boot 4") }
    }

    private companion object {
        fun componentMessage(): String =
            SpringCoreBundle.message("explyt.spring.inspection.boot4.jackson", "JacksonComponent")

        fun mixinMessage(): String =
            SpringCoreBundle.message("explyt.spring.inspection.boot4.jackson", "JacksonMixin")
    }
}
