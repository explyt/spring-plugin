/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.lang.annotation.HighlightSeverity
import org.intellij.lang.annotations.Language

/**
 * Relaxed binding resolves `myKey`, `my-key` and `my_key` to the same property, so a non-canonical key is a style
 * deviation rather than a defect and must not compete with real problems for the user's attention.
 *
 * The highlighting fixtures elsewhere assert the marker, which the platform derives from the severity — but a marker
 * can be made to match by editing the expectation. This test reads the severity back from the produced highlighting,
 * and pins the contrast: the case problem is weak while an unresolved key in the same file is still a warning.
 */
class CanonicalKeyFormSeverityTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    fun testCaseProblemIsWeakWhileUnresolvedKeyStaysAWarning() {
        // An unresolved key is only reported under a prefix some class declares, so the contrast needs a real
        // @ConfigurationProperties owner rather than two arbitrary keys.
        @Language("java") val configurationProperty = """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.prop")
            public class ConfigProperties {
                private String test;
                public void setTest(String test) { this.test = test}
                public String getTest() { return this.test}
            }
        """.trimIndent()
        myFixture.addClass(configurationProperty)
        myFixture.configureByText(
            "application.yaml",
            """
            explyt.prop:
              testCased: some1
              field: some2
            """.trimIndent()
        )

        val severities = myFixture.doHighlighting()
            .filter { it.description != null }
            .associate { it.description to it.severity }

        val caseProblem = severities.entries.single { it.key.contains("canonical form") }
        assertEquals(
            "A non-canonical key must be reported as a weak warning, was ${caseProblem.value}",
            HighlightSeverity.WEAK_WARNING,
            caseProblem.value
        )

        val unresolvedProblems = severities.filterKeys { it.contains("Cannot resolve key property") }
        assertFalse(
            "The fixture must still produce an unresolved-key problem, otherwise the contrast proves nothing",
            unresolvedProblems.isEmpty()
        )
        for ((description, severity) in unresolvedProblems) {
            assertEquals(
                "An unresolved key must stay a warning, was $severity for: $description",
                HighlightSeverity.WARNING,
                severity
            )
        }
    }
}
