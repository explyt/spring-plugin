/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.security.inspections.java

import com.explyt.spring.security.inspections.SpringSecurityAnnotationWithUserDetailsInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.lang.annotation.HighlightSeverity

class WithUserDetailsOwnerTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springSecurityTest_6_0_7,
        TestLibrary.springBootTestAutoConfigure_3_1_1,
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringSecurityAnnotationWithUserDetailsInspection::class.java)
    }

    fun testMissingUserDetailsServiceOnClassThatIsNotSpringTestIsNotReported() {
        configureOwner(classAnnotation = "")
        assertNoHighlightOn("\"missing\"")
    }

    fun testMissingUserDetailsServiceOnSpringTestClassIsReported() {
        configureOwner(classAnnotation = "@$CONTEXT_CONFIGURATION")
        assertHighlightOn("\"missing\"")
    }

    fun testMissingUserDetailsServiceOnSpringBootTestClassIsReported() {
        myFixture.addClass(SPRING_BOOT_TEST_DECLARATION)
        configureOwner(classAnnotation = "@$SPRING_BOOT_TEST")
        assertOwnerCarriesNoOwnContextConfiguration(SPRING_BOOT_TEST)
        assertHighlightOn("\"missing\"")
    }

    fun testMissingUserDetailsServiceOnWebMvcTestClassIsReported() {
        configureOwner(classAnnotation = "@$WEB_MVC_TEST")
        assertOwnerCarriesNoOwnContextConfiguration(WEB_MVC_TEST)
        assertHighlightOn("\"missing\"")
    }

    private fun assertOwnerCarriesNoOwnContextConfiguration(testAnnotation: String) {
        val owner = myFixture.findClass("app.OwnerTest")
        assertNotNull("Precondition: $testAnnotation resolves", myFixture.findClass(testAnnotation))
        assertTrue("Precondition: OwnerTest is annotated with $testAnnotation", owner.hasAnnotation(testAnnotation))
        assertFalse(
            "Precondition: OwnerTest has no @ContextConfiguration of its own",
            owner.hasAnnotation(CONTEXT_CONFIGURATION)
        )
    }

    private fun configureOwner(classAnnotation: String) {
        myFixture.configureByText(
            "OwnerTest.java",
            """
            package app;
            $classAnnotation
            @$WITH_USER_DETAILS(userDetailsServiceBeanName = "missing")
            public class OwnerTest {
                public void test() {}
            }
            """.trimIndent()
        )
    }

    private fun highlightsOn(text: String) =
        myFixture.doHighlighting().filter { it.severity >= HighlightSeverity.WEAK_WARNING && it.text == text }

    private fun assertNoHighlightOn(text: String) {
        val found = highlightsOn(text)
        assertTrue("Expected no bean-name problem on $text, got ${found.map { it.description }}", found.isEmpty())
    }

    private fun assertHighlightOn(text: String) {
        val found = highlightsOn(text)
        assertTrue("Expected a bean-name problem on $text", found.isNotEmpty())
    }

    private companion object {
        const val WITH_USER_DETAILS = "org.springframework.security.test.context.support.WithUserDetails"
        const val CONTEXT_CONFIGURATION = "org.springframework.test.context.ContextConfiguration"
        const val SPRING_BOOT_TEST = "org.springframework.boot.test.context.SpringBootTest"
        const val WEB_MVC_TEST = "org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest"
        val SPRING_BOOT_TEST_DECLARATION = """
            package org.springframework.boot.test.context;
            @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
            @java.lang.annotation.Inherited
            @org.springframework.test.context.BootstrapWith
            public @interface SpringBootTest {}
        """.trimIndent()
    }
}
