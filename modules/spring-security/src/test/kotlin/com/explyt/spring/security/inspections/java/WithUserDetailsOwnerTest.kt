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
    }
}
