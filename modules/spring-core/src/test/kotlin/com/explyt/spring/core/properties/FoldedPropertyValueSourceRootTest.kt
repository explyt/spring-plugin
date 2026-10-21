/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary

/**
 * A key defined in both `src/main/resources` and `src/test/resources` must fold to the production value.
 *
 * Configuration files are collected from dependencies as well as dependents (issue #381), so a Gradle test
 * source-set's `application.yaml` is in scope for the module it tests. Both files are profile-less and share the
 * name `application.yaml`, which tied every rank the ordering had, leaving the winner to index iteration order:
 * production code could fold to a test value, and flip between sessions.
 *
 * This needs a fixture with a real test source root — with one production root the bug cannot be expressed.
 */
class FoldedPropertyValueSourceRootTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testProductionValueWinsOverTestValueOfTheSameFileName() {
        addFileToModule(module, "application.yaml", "management:\n  server:\n    port: 9086\n")
        addTestSourceFileToModule(module, "application.yaml", "management:\n  server:\n    port: 0\n")

        assertBothDefinitionsAreInScope()
        assertEquals("9086", resolvedValue())
    }

    /**
     * Same two files, created in the opposite order. Their relative order is the only thing that decided the winner
     * before the fix, so the pair proves the outcome is now independent of it — one of them necessarily passed by
     * luck beforehand, which is why both assert the precondition that the test file is in scope at all.
     */
    fun testProductionValueWinsRegardlessOfWhichFileIsIndexedFirst() {
        addTestSourceFileToModule(module, "application.yaml", "management:\n  server:\n    port: 0\n")
        addFileToModule(module, "application.yaml", "management:\n  server:\n    port: 9086\n")

        assertBothDefinitionsAreInScope()
        assertEquals("9086", resolvedValue())
    }

    /**
     * A test source root is deprioritised, never excluded: dropping it would leave a key defined only there with no
     * folded value at all, which is a regression of issue #381 rather than a fix.
     */
    fun testTestValueIsUsedWhenProductionDoesNotDefineTheKey() {
        addTestSourceFileToModule(module, "application.yaml", "management:\n  server:\n    port: 0\n")

        assertEquals("0", resolvedValue())
    }

    /**
     * Without this the pair above degrades into "production wins because nothing competes with it": if the test
     * source root ever falls out of scope, both would still report `9086` and silently stop testing anything.
     */
    private fun assertBothDefinitionsAreInScope() {
        val definedValues = DefinedConfigurationPropertiesSearch.getInstance(project)
            .findProperties(module, "management.server.port")
            .mapNotNull { it.value }
            .sorted()

        assertEquals(listOf("0", "9086"), definedValues)
    }

    private fun resolvedValue(): String? =
        FoldedPropertyValue.resolve(module, "management.server.port")?.value
}
