/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.actionSystem.IdeActions

/** Decides which performed actions are worth a breadcrumb, and under which name. */
object ActionBreadcrumbPolicy {

    private const val OWN_PACKAGE_PREFIX = "com.explyt."

    /**
     * Editor text input and caret movement, which a user performs continuously.
     *
     * They are dropped at the producer rather than at send time, because Sentry keeps only the last 100 breadcrumbs:
     * a burst of keystrokes evicts the navigation entries that actually explain a failure before the report is built.
     *
     * `$Delete`, `$Cut` and `$Paste` are deliberately absent. Those ids are place-agnostic — in the Project View they
     * delete or move files, the most valuable context for a PSI/VFS failure — and recorded events show a `place` of
     * `keyboard shortcut` for them, so the editor case cannot be told apart.
     */
    private val editorNoiseActionIds = setOf(
        IdeActions.ACTION_EDITOR_CUT,
        IdeActions.ACTION_EDITOR_COPY,
        IdeActions.ACTION_EDITOR_PASTE,
        IdeActions.ACTION_EDITOR_PASTE_SIMPLE,
        IdeActions.ACTION_EDITOR_PASTE_FROM_HISTORY,
        IdeActions.ACTION_EDITOR_DELETE,
        IdeActions.ACTION_EDITOR_DELETE_TO_WORD_START,
        IdeActions.ACTION_EDITOR_DELETE_TO_WORD_END,
        IdeActions.ACTION_EDITOR_DELETE_LINE,
        IdeActions.ACTION_EDITOR_ENTER,
        IdeActions.ACTION_EDITOR_START_NEW_LINE,
        IdeActions.ACTION_EDITOR_BACKSPACE,
        IdeActions.ACTION_EDITOR_TAB,
        IdeActions.ACTION_EDITOR_INDENT_SELECTION,
        IdeActions.ACTION_EDITOR_UNINDENT_SELECTION,
        IdeActions.ACTION_EDITOR_EMACS_TAB,
        IdeActions.ACTION_EDITOR_MOVE_CARET_UP,
        IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN,
        IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT,
        IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT,
        IdeActions.ACTION_EDITOR_MOVE_CARET_UP_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_LINE_START,
        IdeActions.ACTION_EDITOR_MOVE_LINE_END,
        IdeActions.ACTION_EDITOR_MOVE_LINE_START_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_MOVE_LINE_END_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_NEXT_WORD,
        IdeActions.ACTION_EDITOR_PREVIOUS_WORD,
        IdeActions.ACTION_EDITOR_NEXT_WORD_WITH_SELECTION,
        IdeActions.ACTION_EDITOR_PREVIOUS_WORD_WITH_SELECTION,
        IdeActions.ACTION_COPY,
        IdeActions.ACTION_SELECT_ALL,
    )

    /** Returns whether an action named [actionId] carries enough signal to be recorded. */
    fun shouldRecord(actionId: String?): Boolean =
        !actionId.isNullOrBlank() && actionId !in editorNoiseActionIds

    /**
     * The name to record for a performed action, or `null` when it should be skipped.
     *
     * Most of our own toolbar and gutter actions are anonymous `object : AnAction()` declarations, which
     * `ActionManager` cannot name. Falling back to their class keeps the plugin's own actions visible; doing the same
     * for a third-party action would only add an unsearchable class name, so those are dropped instead.
     */
    fun breadcrumbName(actionId: String?, actionClass: Class<*>): String? = when {
        !actionId.isNullOrBlank() -> actionId.takeIf { shouldRecord(it) }
        // Keeps the enclosing class for an anonymous action, whose own simple name is empty.
        actionClass.name.startsWith(OWN_PACKAGE_PREFIX) -> actionClass.name.substringAfterLast('.')
        else -> null
    }
}
