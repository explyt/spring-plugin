/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BeanContextSelectorTest {

    private val application = BeanApplicationIdentity("com.explyt.demo.App", "demo.main", "app-source")

    @Test
    fun `AUTO does not union two matching contexts`() {
        val contexts = listOf("ctx-a", "ctx-b").map { context(it) }

        val failure = assertThrows(BeanQueryException::class.java) {
            BeanContextSelector.select(application, contexts, BeanSourcePreference.AUTO, null)
        }

        assertEquals("CONTEXT_REQUIRED", failure.problem.code)
        assertEquals(setOf("ctx-a", "ctx-b"), failure.problem.choices.map { it["contextId"] }.toSet())
    }

    @Test
    fun `an explicit context of another application is a mismatch, not an absence`() {
        val foreign = context("ctx-foreign", applicationClassName = "com.explyt.demo.Other")

        val failure = assertThrows(BeanQueryException::class.java) {
            BeanContextSelector.select(application, listOf(foreign), BeanSourcePreference.AUTO, "ctx-foreign")
        }

        assertEquals("CONTEXT_APPLICATION_MISMATCH", failure.problem.code)
    }

    @Test
    fun `an explicit context that is not loaded never falls back to the static model`() {
        val failure = assertThrows(BeanQueryException::class.java) {
            BeanContextSelector.select(application, listOf(context("ctx-a")), BeanSourcePreference.AUTO, "ctx-gone")
        }

        assertEquals("NATIVE_CONTEXT_NOT_AVAILABLE", failure.problem.code)
    }

    @Test
    fun `NATIVE without a matching snapshot fails instead of answering from the static model`() {
        val failure = assertThrows(BeanQueryException::class.java) {
            BeanContextSelector.select(application, emptyList(), BeanSourcePreference.NATIVE, null)
        }

        assertEquals("NATIVE_CONTEXT_NOT_AVAILABLE", failure.problem.code)
    }

    @Test
    fun `AUTO falls back to the static model and says why`() {
        val selection = BeanContextSelector.select(application, emptyList(), BeanSourcePreference.AUTO, null)

        assertEquals(BeanModelSource.STATIC, selection.source)
        assertNull(selection.nativeContext)
        assertEquals(
            setOf("STATIC_CONTEXT_APPROXIMATE", "NO_MATCHING_NATIVE_SNAPSHOT"),
            selection.limitations
        )
    }

    @Test
    fun `a single matching context is selected and marked as a snapshot`() {
        val selected = context("ctx-a")
        val other = context("ctx-other", applicationClassName = "com.explyt.demo.Other")

        val selection = BeanContextSelector.select(application, listOf(selected, other), BeanSourcePreference.AUTO, null)

        assertEquals(BeanModelSource.NATIVE_SNAPSHOT, selection.source)
        assertEquals(selected, selection.nativeContext)
        assertEquals(setOf("NATIVE_SNAPSHOT_NOT_LIVE"), selection.limitations)
    }

    /** A Kotlin `main` facade is not the `@SpringBootApplication` FQN, so such a root is matched by its source file. */
    @Test
    fun `a context without an application class matches by main source identity`() {
        val bySource = context("ctx-kotlin", applicationClassName = null)

        val selection = BeanContextSelector.select(application, listOf(bySource), BeanSourcePreference.NATIVE, null)

        assertEquals(bySource, selection.nativeContext)
    }

    @Test
    fun `an unproven identity is not a match even when its fields line up`() {
        val unproven = context("ctx-debug", identityProven = false)

        val failure = assertThrows(BeanQueryException::class.java) {
            BeanContextSelector.select(application, listOf(unproven), BeanSourcePreference.NATIVE, null)
        }

        assertEquals("NATIVE_CONTEXT_NOT_AVAILABLE", failure.problem.code)
    }

    @Test
    fun `STATIC ignores loaded contexts entirely`() {
        val selection =
            BeanContextSelector.select(application, listOf(context("ctx-a")), BeanSourcePreference.STATIC, null)

        assertEquals(BeanModelSource.STATIC, selection.source)
        assertNull(selection.nativeContext)
        assertEquals(setOf("STATIC_CONTEXT_APPROXIMATE"), selection.limitations)
    }

    @Test
    fun `STATIC with a contextId is a rejected argument, not a silent native answer`() {
        val failure = assertThrows(BeanQueryException::class.java) {
            BeanContextSelector.select(application, listOf(context("ctx-a")), BeanSourcePreference.STATIC, "ctx-a")
        }

        assertEquals("INVALID_ARGUMENT", failure.problem.code)
    }

    private fun context(
        id: String,
        applicationClassName: String? = application.className,
        mainSourceKey: String? = application.mainSourceKey,
        identityProven: Boolean = true
    ) = NativeBeanContext(id, id, "/internal/$id", applicationClassName, mainSourceKey, identityProven)
}
