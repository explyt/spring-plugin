/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary

/**
 * Completion of the key of `logging.level.<suffix>`.
 *
 * Resolution, inspection and completion of a key are three independent paths, so reworking the references that resolve
 * the suffix has to be shown not to cost the completion served by the same references.
 */
class LoggingLevelKeyCompletionTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBoot_3_1_1, TestLibrary.springBootAutoConfigure_3_1_1)

    fun testGroupsAndPackagesAreOffered() {
        myFixture.configureByText("application.properties", "logging.level.<caret>")

        val variants = myFixture.completeBasic().map { it.lookupString }

        assertContainsElements(variants, "root", "sql", "web")
        assertContainsElements(variants, "org", "java")
    }

    fun testPackagesAreOfferedInsideAPackageChain() {
        myFixture.configureByText("application.properties", "logging.level.org.<caret>")

        val variants = myFixture.completeBasic().map { it.lookupString }

        assertContainsElements(variants, "springframework")
    }

    fun testYamlGroupsAreOffered() {
        myFixture.configureByText("application.yaml", "logging:\n  level:\n    <caret>")

        val variants = myFixture.completeBasic().map { it.lookupString }

        assertContainsElements(variants, "root", "sql", "web")
    }
}
