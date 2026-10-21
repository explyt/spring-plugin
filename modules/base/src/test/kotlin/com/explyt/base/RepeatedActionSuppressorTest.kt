/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class RepeatedActionSuppressorTest {

    /** A held-down key must occupy one slot in Sentry's bounded queue, not one slot per repeat. */
    @Test
    fun `suppresses a run of the same action`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.onAction("EditorChooseLookupItem", "keyboard shortcut", Date(1)).record)
        assertFalse(suppressor.onAction("EditorChooseLookupItem", "keyboard shortcut", Date(2)).record)
        assertFalse(suppressor.onAction("EditorChooseLookupItem", "keyboard shortcut", Date(3)).record)
    }

    @Test
    fun `records the same action again after a different one`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.onAction("GotoDeclaration", "Editor", Date(1)).record)
        assertTrue(suppressor.onAction("SearchEverywhere", "Editor", Date(2)).record)
        assertTrue(suppressor.onAction("GotoDeclaration", "Editor", Date(3)).record)
    }

    /** The same action from another place is a distinct user step and must stay visible. */
    @Test
    fun `distinguishes places`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.onAction("\$Delete", "ProjectViewPopup", Date(1)).record)
        assertTrue(suppressor.onAction("\$Delete", "keyboard shortcut", Date(2)).record)
    }

    @Test
    fun `handles a missing place`() {
        val suppressor = RepeatedActionSuppressor()

        assertTrue(suppressor.onAction("GotoDeclaration", null, Date(1)).record)
        assertFalse(suppressor.onAction("GotoDeclaration", null, Date(2)).record)
    }

    /**
     * Editor input is never recorded, yet it separates two recorded actions into two steps: without that, a repeat
     * performed twenty minutes after typing would vanish as a repeat of the first.
     */
    @Test
    fun `an unrecorded action ends the run`() {
        val suppressor = RepeatedActionSuppressor()
        suppressor.onAction("GotoDeclaration", "Editor", Date(1))

        assertNull("Nothing was suppressed, so nothing is reported", suppressor.breakRun())
        assertTrue(suppressor.onAction("GotoDeclaration", "Editor", Date(2)).record)
    }

    /** The suppressed repeats come back once their run ends, with their number and the time of the last one. */
    @Test
    fun `an ended run reports its suppressed repeats`() {
        val suppressor = RepeatedActionSuppressor()
        suppressor.onAction("GotoDeclaration", "Editor", Date(1))
        suppressor.onAction("GotoDeclaration", "Editor", Date(2))
        suppressor.onAction("GotoDeclaration", "Editor", Date(3))

        val decision = suppressor.onAction("SearchEverywhere", "Editor", Date(4))

        assertTrue(decision.record)
        assertEquals(
            RepeatedActionSuppressor.SuppressedRepeats("GotoDeclaration", "Editor", 2, Date(3)),
            decision.endedRun
        )
        assertEquals(null, suppressor.breakRun())
    }
}
