/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.actionSystem.IdeActions

/** Keeps high-frequency editor input out of action breadcrumbs while preserving navigation context. */
object ActionBreadcrumbPolicy {

    private val editorInputActionIds = setOf(
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
        IdeActions.ACTION_COPY,
        IdeActions.ACTION_CUT,
        IdeActions.ACTION_DELETE,
        IdeActions.ACTION_PASTE,
        IdeActions.ACTION_SELECT_ALL,
    )

    /** Returns whether [actionId] is a useful, registered action to record. */
    fun shouldRecord(actionId: String?): Boolean =
        !actionId.isNullOrBlank() && actionId !in editorInputActionIds
}
