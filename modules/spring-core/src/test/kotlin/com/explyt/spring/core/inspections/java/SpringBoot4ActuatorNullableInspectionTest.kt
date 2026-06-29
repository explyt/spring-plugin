/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4ActuatorNullableInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4ActuatorNullableInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        addStubs()
        myFixture.enableInspections(SpringBoot4ActuatorNullableInspection::class.java)
    }

    private fun addStubs() {
        myFixture.addClass(
            """
            package org.springframework.boot.actuate.endpoint.annotation;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
            public @interface ReadOperation { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.lang;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
            public @interface Nullable { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.jspecify.annotations;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
            public @interface Nullable { }
            """.trimIndent()
        )
    }

    fun testSpringNullableOnOperationReported() {
        myFixture.configureByText(
            "MyEndpoint.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.lang.Nullable;
            
            public class MyEndpoint {
                @ReadOperation
                public Object op(<warning descr="Actuator endpoint parameters use JSpecify @Nullable in Spring Boot 4. Use org.jspecify.annotations.Nullable">@Nullable</warning> String name) {
                    return name;
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyEndpoint.java")
    }

    fun testJSpecifyNullableNotReported() {
        myFixture.configureByText(
            "MyEndpoint.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.jspecify.annotations.Nullable;
            
            public class MyEndpoint {
                @ReadOperation
                public Object op(@Nullable String name) {
                    return name;
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyEndpoint.java")
    }

    fun testSpringNullableOutsideOperationNotReported() {
        myFixture.configureByText(
            "MyService.java",
            """
            import org.springframework.lang.Nullable;
            
            public class MyService {
                public Object op(@Nullable String name) {
                    return name;
                }
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyService.java")
    }

    fun testQuickFixReplacesNullable() {
        myFixture.configureByText(
            "MyEndpoint.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.lang.Nullable;
            
            public class MyEndpoint {
                @ReadOperation
                public Object op(@Null<caret>able String name) {
                    return name;
                }
            }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("Nullable") && it.text.contains("Annotate") }
            ?: myFixture.availableIntentions.firstOrNull { it.text.contains("jspecify") || it.text.contains("Nullable") }
        requireNotNull(intention) { "Replace-annotation fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "jspecify import should be added:\n$result",
            result.contains("import org.jspecify.annotations.Nullable;")
        )
    }
}
