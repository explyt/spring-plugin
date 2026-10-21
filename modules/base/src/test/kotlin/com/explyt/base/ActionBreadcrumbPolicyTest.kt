/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.actionSystem.IdeActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionBreadcrumbPolicyTest {

    @Test
    fun `drops editor text input`() {
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_BACKSPACE))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_COPY))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_PASTE))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_ENTER))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_TAB))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_COPY))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_SELECT_ALL))
    }

    /** Caret movement is the highest-volume noise: it evicts useful entries from Sentry's bounded queue. */
    @Test
    fun `drops caret movement`() {
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_MOVE_CARET_UP))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_PREVIOUS_WORD_WITH_SELECTION))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_EDITOR_MOVE_LINE_END))
    }

    /**
     * `$Delete`, `$Cut` and `$Paste` are place-agnostic: in the Project View they delete or move files, which is the
     * most valuable context for a PSI/VFS failure. Sentry data shows their `place` is often just
     * `keyboard shortcut`, so the editor case cannot be told apart and these must stay recorded.
     */
    @Test
    fun `keeps place-agnostic file manipulation`() {
        assertTrue(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_DELETE))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_CUT))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_PASTE))
    }

    @Test
    fun `keeps navigation and completion`() {
        assertTrue(ActionBreadcrumbPolicy.shouldRecord("GotoDeclaration"))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord("SearchEverywhere"))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord("ReopenProjectAction"))
    }

    @Test
    fun `names a registered action by its id`() {
        assertEquals(
            "GotoDeclaration",
            ActionBreadcrumbPolicy.breadcrumbName("GotoDeclaration", FOREIGN_ACTION_CLASS)
        )
    }

    @Test
    fun `drops a filtered action even when its id is registered`() {
        assertNull(ActionBreadcrumbPolicy.breadcrumbName("EditorBackSpace", FOREIGN_ACTION_CLASS))
    }

    /** A third-party action without an id carries no searchable signal, so a class name would only add noise. */
    @Test
    fun `drops a foreign action without an id`() {
        assertNull(ActionBreadcrumbPolicy.breadcrumbName(null, FOREIGN_ACTION_CLASS))
        assertNull(ActionBreadcrumbPolicy.breadcrumbName("", FOREIGN_ACTION_CLASS))
        assertNull(ActionBreadcrumbPolicy.breadcrumbName("   ", FOREIGN_ACTION_CLASS))
    }

    /**
     * Our own toolbar and gutter actions are declared as anonymous `object : AnAction()`, so `ActionManager` has no id
     * for them. Dropping them would blind the telemetry to exactly the plugin actions it exists to observe.
     */
    @Test
    fun `names our own action without an id by its class`() {
        assertEquals(
            $$"ActionBreadcrumbPolicyTest$OwnAction",
            ActionBreadcrumbPolicy.breadcrumbName(null, OwnAction::class.java)
        )
    }

    private class OwnAction

    private companion object {
        /** Stands in for a third-party action class, which lives outside the `com.explyt` package. */
        private val FOREIGN_ACTION_CLASS: Class<*> = java.util.Date::class.java
    }
}
