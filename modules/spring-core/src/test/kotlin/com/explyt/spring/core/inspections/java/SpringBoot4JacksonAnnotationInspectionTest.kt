/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4JacksonAnnotationInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4JacksonAnnotationInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        addStubs()
        myFixture.enableInspections(SpringBoot4JacksonAnnotationInspection::class.java)
    }

    private fun addStubs() {
        myFixture.addClass(
            """
            package org.springframework.boot.jackson;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface JsonComponent { Class<?>[] value() default {}; }
            """.trimIndent()
        )
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
            public @interface JsonMixin { Class<?>[] value() default {}; }
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
    }

    fun testJsonComponentReported() {
        myFixture.configureByText(
            "MyComponent.java",
            """
            import org.springframework.boot.jackson.JsonComponent;
            
            <warning descr="This annotation was renamed in Spring Boot 4. Use '@JacksonComponent'">@JsonComponent</warning>
            public class MyComponent { }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyComponent.java")
    }

    fun testJsonMixinReported() {
        myFixture.configureByText(
            "MyMixin.java",
            """
            import org.springframework.boot.jackson.JsonMixin;
            
            <warning descr="This annotation was renamed in Spring Boot 4. Use '@JacksonMixin'">@JsonMixin(String.class)</warning>
            public class MyMixin { }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyMixin.java")
    }

    fun testJacksonComponentNotReported() {
        myFixture.configureByText(
            "MyComponent.java",
            """
            import org.springframework.boot.jackson.JacksonComponent;
            
            @JacksonComponent
            public class MyComponent { }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyComponent.java")
    }

    fun testQuickFixReplacesAnnotationPreservingAttributes() {
        myFixture.configureByText(
            "MyMixin.java",
            """
            import org.springframework.boot.jackson.JsonMixin;
            
            @Json<caret>Mixin(String.class)
            public class MyMixin { }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("JacksonMixin") }
        requireNotNull(intention) { "Replace-annotation fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue("new annotation should be applied:\n$result", result.contains("@JacksonMixin"))
        assertTrue("attribute should be preserved:\n$result", result.contains("String.class"))
        assertFalse("old annotation use should be removed:\n$result", result.contains("@JsonMixin"))
    }
}
