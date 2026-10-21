/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiPolyVariantReference

/**
 * Navigation on the key of `logging.level.<suffix>`.
 *
 * Spring describes both kinds of suffix itself: `logging.level.keys` declares the logging groups it ships, and its
 * `logger-name` provider says anything else names a logger, that is a package or a class.
 */
class LoggingLevelKeyReferenceTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springBootAutoConfigure_3_1_1)

    fun testPackageSuffixResolvesToPackage() {
        myFixture.configureByText("application.properties", "logging.level.org.springframework=DEBUG")

        assertResolvesToPackage("springframework", "org.springframework")
    }

    fun testNestedPackageSuffixResolvesToPackage() {
        myFixture.configureByText("application.properties", "logging.level.org.springframework.boot=DEBUG")

        assertResolvesToPackage("boot", "org.springframework.boot")
    }

    fun testFirstSegmentOfPackageSuffixStillResolves() {
        myFixture.configureByText("application.properties", "logging.level.org.springframework=DEBUG")

        assertResolvesToPackage("org", "org")
    }

    fun testRootGroupResolvesToHintValue() = assertResolvesToDeclaredGroup("root")

    fun testSqlGroupResolvesToHintValue() = assertResolvesToDeclaredGroup("sql")

    fun testWebGroupResolvesToHintValue() = assertResolvesToDeclaredGroup("web")

    fun testUnknownSuffixResolvesToNothing() {
        myFixture.configureByText("application.properties", "logging.level.no.such.pkg=DEBUG")

        val targets = targetsAt("pkg")
        assertEquals("a logger name naming nothing must offer no target, got: $targets", 0, targets.size)
        assertTrue(
            "an unknown logger name is legal and must not be reported",
            referenceAt("pkg")?.isSoft != false
        )
    }

    fun testYamlPackageSuffixResolvesToPackage() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                org.springframework: DEBUG
            """.trimIndent()
        )

        assertResolvesToPackage("springframework", "org.springframework")
    }

    fun testYamlRootGroupResolvesToHintValue() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              level:
                root: DEBUG
            """.trimIndent()
        )

        assertDeclaredGroupResolved("root")
    }

    fun testYamlFlatKeyPackageSuffixResolvesToPackage() {
        myFixture.configureByText("application.yaml", "logging.level.org.springframework: DEBUG")

        assertResolvesToPackage("springframework", "org.springframework")
    }

    fun testYamlFlatKeyGroupResolvesToHintValue() {
        myFixture.configureByText("application.yaml", "logging.level.root: DEBUG")

        assertDeclaredGroupResolved("root")
    }

    fun testUserDefinedGroupResolvesToItsDefinition() {
        myFixture.configureByText(
            "application.properties",
            """
            logging.group.mine=com.example.a,com.example.b
            logging.level.mine=DEBUG
            """.trimIndent()
        )

        val targets = targetsAtLast("mine")

        assertEquals(
            "a user-defined group is one definition, got: ${targets.map { describe(it) }}",
            1, targets.size
        )
        assertEquals(
            "the group must navigate to its `logging.group` definition",
            "logging.group.mine=com.example.a,com.example.b", targets.single().text
        )
    }

    fun testYamlUserDefinedGroupResolvesToItsDefinition() {
        myFixture.configureByText(
            "application.yaml",
            """
            logging:
              group:
                mine: com.example.a,com.example.b
              level:
                mine: DEBUG
            """.trimIndent()
        )

        val targets = targetsAtLast("mine")

        assertEquals(
            "a user-defined group is one definition, got: ${targets.map { describe(it) }}",
            1, targets.size
        )
        assertEquals(
            "the group must navigate to its `logging.group` definition",
            "mine: com.example.a,com.example.b", targets.single().text
        )
    }

    fun testUserDefinedGroupWinsOverPackageOfTheSameName() {
        myFixture.addClass("package org.example; public class A {}")
        myFixture.configureByText(
            "application.properties",
            """
            logging.group.org=com.example.a
            logging.level.org=DEBUG
            """.trimIndent()
        )

        val targets = targetsAtLast("org")

        assertEquals(
            "the more specific group definition must be the only target, got: ${targets.map { describe(it) }}",
            1, targets.size
        )
        assertEquals("logging.group.org=com.example.a", targets.single().text)
    }

    private fun assertResolvesToDeclaredGroup(group: String) {
        myFixture.configureByText("application.properties", "logging.level.$group=DEBUG")

        assertDeclaredGroupResolved(group)
    }

    private fun assertDeclaredGroupResolved(group: String) {
        val targets = targetsAt(group)

        assertEquals(
            "a declared group is one declaration, got: ${targets.map { describe(it) }}",
            1, targets.size
        )
        val literal = (targets.single() as? JsonProperty)?.value as? JsonStringLiteral
        assertEquals(
            "the group must navigate to its `logging.level.keys` hint literal, got: ${describe(targets.single())}",
            group, literal?.value
        )
    }

    private fun assertResolvesToPackage(segment: String, expectedPackage: String) {
        val targets = targetsAt(segment)

        assertEquals(
            "a logger name is one package, got: ${targets.map { describe(it) }}",
            1, targets.size
        )
        assertEquals(
            "the logger name must navigate to its package",
            expectedPackage, (targets.single() as? PsiPackage)?.qualifiedName
        )
    }

    /** Resolution as Ctrl+Click performs it: through whatever reference the platform picks at the caret. */
    private fun targetsAt(segment: String): List<PsiElement> {
        val reference = referenceAt(segment) ?: return emptyList()
        return when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(reference.resolve())
        }
    }

    private fun targetsAtLast(segment: String): List<PsiElement> {
        val offset = myFixture.file.text.lastIndexOf(segment) + segment.length - 1
        val reference = myFixture.file.findReferenceAt(offset) ?: return emptyList()
        return when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(reference.resolve())
        }
    }

    private fun referenceAt(segment: String) =
        myFixture.file.findReferenceAt(myFixture.file.text.indexOf(segment) + segment.length - 1)

    private fun describe(element: PsiElement) =
        "${element::class.java.simpleName}[${element.text}] in ${element.containingFile?.virtualFile?.path}"
}
