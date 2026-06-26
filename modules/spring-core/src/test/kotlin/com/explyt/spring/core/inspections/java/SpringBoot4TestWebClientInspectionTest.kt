/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4TestWebClientInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4TestWebClientInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        addStubs()
        myFixture.enableInspections(SpringBoot4TestWebClientInspection::class.java)
    }

    private fun addStubs() {
        myFixture.addClass(
            """
            package org.springframework.boot.test.context;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface SpringBootTest { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.boot.webmvc.test.autoconfigure;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface AutoConfigureMockMvc { }
            """.trimIndent()
        )
        myFixture.addClass(
            """
            package org.springframework.boot.resttestclient.autoconfigure;
            import java.lang.annotation.*;
            @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
            public @interface AutoConfigureTestRestTemplate { }
            """.trimIndent()
        )
        myFixture.addClass("package org.springframework.test.web.servlet; public class MockMvc { }")
        myFixture.addClass("package org.springframework.boot.resttestclient; public class TestRestTemplate { }")
    }

    fun testMockMvcWithoutAutoConfigureReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.test.context.SpringBootTest;
            import org.springframework.test.web.servlet.MockMvc;
            
            @SpringBootTest
            public class <warning descr="@SpringBootTest no longer auto-configures MockMvc in Spring Boot 4. Add @AutoConfigureMockMvc">MyTest</warning> {
                MockMvc mockMvc;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testMockMvcWithAutoConfigureNotReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.test.context.SpringBootTest;
            import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
            import org.springframework.test.web.servlet.MockMvc;
            
            @SpringBootTest
            @AutoConfigureMockMvc
            public class MyTest {
                MockMvc mockMvc;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testNoSpringBootTestNotReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.test.web.servlet.MockMvc;
            
            public class MyTest {
                MockMvc mockMvc;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testQuickFixAddsAutoConfigureMockMvc() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.test.context.SpringBootTest;
            import org.springframework.test.web.servlet.MockMvc;
            
            @SpringBootTest
            public class My<caret>Test {
                MockMvc mockMvc;
            }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("AutoConfigureMockMvc") }
        requireNotNull(intention) { "Add-annotation fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue("annotation should be added:\n$result", result.contains("@AutoConfigureMockMvc"))
    }
}
