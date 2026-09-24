/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatedActionSuppressorTest {

    /** A held-down key must occupy one slot in Sentry's bounded queue, not one slot per repeat. */
    @Test
    fun `suppresses a run of the same action`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.shouldRecord("EditorChooseLookupItem", "keyboard shortcut"))
        assertFalse(suppressor.shouldRecord("EditorChooseLookupItem", "keyboard shortcut"))
        assertFalse(suppressor.shouldRecord("EditorChooseLookupItem", "keyboard shortcut"))
    }

    @Test
    fun `records the same action again after a different one`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.shouldRecord("GotoDeclaration", "Editor"))
        assertTrue(suppressor.shouldRecord("SearchEverywhere", "Editor"))
        assertTrue(suppressor.shouldRecord("GotoDeclaration", "Editor"))
    }

    /** The same action from another place is a distinct user step and must stay visible. */
    @Test
    fun `distinguishes places`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.shouldRecord("\$Delete", "ProjectViewPopup"))
        assertTrue(suppressor.shouldRecord("\$Delete", "keyboard shortcut"))
    }

    @Test
    fun `handles a missing place`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.shouldRecord("GotoDeclaration", null))
        assertFalse(suppressor.shouldRecord("GotoDeclaration", null))
    }
}
