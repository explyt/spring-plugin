/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.testFramework.DumbModeTestUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue

/**
 * Changes an IDE makes all the time without touching the Spring model: a note written next to the sources, the PSI
 * caches dropped by a native snapshot import, an indexing pass. A page revision that moves on them makes a client's
 * continuation fail with `RESULT_CHANGED` although every page would describe the same answer.
 */
enum class UnrelatedPsiChange(val description: String) {
    NOTE_EDITED("a markdown note written and edited"),
    PSI_CACHES_DROPPED("PsiManager.dropPsiCaches()"),
    DUMB_MODE_CYCLED("entering and leaving dumb mode");

    fun applyTo(project: Project, notes: () -> PsiFile) {
        val before = PsiModificationTracker.getInstance(project).modificationCount
        when (this) {
            NOTE_EDITED -> editNote(project, notes())
            PSI_CACHES_DROPPED -> PsiManager.getInstance(project).dropPsiCaches()
            DUMB_MODE_CYCLED -> DumbModeTestUtils.runInDumbModeSynchronously(project) {}
        }
        assertTrue(
            "Precondition: $description must move the global PSI modification count",
            PsiModificationTracker.getInstance(project).modificationCount != before
        )
    }

    private fun editNote(project: Project, note: PsiFile) {
        val documents = PsiDocumentManager.getInstance(project)
        val document = documents.getDocument(note) ?: error("The note ${note.name} has no document")
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.textLength, "\nAnother line of the note.")
            documents.commitDocument(document)
        }
    }
}

fun assertTruncatedFirstPage(tool: String, first: JsonNode) {
    assertEquals("Precondition: $tool page 1 is served, got $first", "OK", first["status"]?.asText())
    assertTrue("Precondition: $tool page 1 must be truncated, got $first", first["truncated"]?.asBoolean() == true)
    assertFalse("Precondition: $tool page 1 continues, got $first", first["nextOffset"]?.isNull ?: true)
}

fun assertContinued(tool: String, change: String, second: JsonNode) {
    assertEquals(
        "$tool page 2 after $change must continue page 1's revision, the Spring model did not change; got $second",
        "OK", second["status"]?.asText()
    )
}

fun assertResultChanged(tool: String, change: String, second: JsonNode) {
    assertEquals("$tool page 2 after $change must reject page 1's revision, got $second", "ERROR", second["status"]?.asText())
    assertEquals("RESULT_CHANGED", second["error"]?.get("code")?.asText())
}
