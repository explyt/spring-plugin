/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringBoot4TestRestTemplatePackageInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringBoot4TestRestTemplatePackageInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_4_0_0
    )

    override fun setUp() {
        super.setUp()
        myFixture.addClass("package org.springframework.boot.test.web.client; public class TestRestTemplate { }")
        myFixture.addClass("package org.springframework.boot.resttestclient; public class TestRestTemplate { }")
        myFixture.enableInspections(SpringBoot4TestRestTemplatePackageInspection::class.java)
    }

    fun testLegacyImportReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            <warning descr="TestRestTemplate moved to org.springframework.boot.resttestclient in Spring Boot 4">import org.springframework.boot.test.web.client.TestRestTemplate;</warning>
            
            public class MyTest {
                TestRestTemplate template;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testNewImportNotReported() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.resttestclient.TestRestTemplate;
            
            public class MyTest {
                TestRestTemplate template;
            }
            """.trimIndent()
        )
        myFixture.testHighlighting("MyTest.java")
    }

    fun testQuickFixUpdatesImport() {
        myFixture.configureByText(
            "MyTest.java",
            """
            import org.springframework.boot.test.web.client.TestRest<caret>Template;
            
            public class MyTest {
                TestRestTemplate template;
            }
            """.trimIndent()
        )
        val intention = myFixture.availableIntentions
            .firstOrNull { it.text.contains("TestRestTemplate") || it.text.contains("Spring Boot 4") }
        requireNotNull(intention) { "Import migration fix not found; available: " + myFixture.availableIntentions.map { it.text } }
        myFixture.launchAction(intention)
        val result = myFixture.file.text
        assertTrue(
            "import should be migrated:\n$result",
            result.contains("import org.springframework.boot.resttestclient.TestRestTemplate;")
        )
        assertFalse(
            "old import should be replaced:\n$result",
            result.contains("import org.springframework.boot.test.web.client.TestRestTemplate;")
        )
    }
}
