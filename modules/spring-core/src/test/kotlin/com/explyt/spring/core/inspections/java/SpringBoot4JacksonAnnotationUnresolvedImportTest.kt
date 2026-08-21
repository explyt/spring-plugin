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
