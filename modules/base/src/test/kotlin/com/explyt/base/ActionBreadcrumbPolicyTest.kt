/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.actionSystem.IdeActions
import io.sentry.Breadcrumb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ActionBreadcrumbPolicyTest {

    @Test
    fun `drops editor input actions but keeps navigation and completion`() {
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(null))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord("EditorBackSpace"))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord("EditorCopy"))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord("EditorPaste"))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord("EditorEnter"))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord("EditorTab"))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_COPY))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_PASTE))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(IdeActions.ACTION_SELECT_ALL))

        assertTrue(ActionBreadcrumbPolicy.shouldRecord("EditorDown"))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord("EditorChooseLookupItem"))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord("GotoDeclaration"))
        assertTrue(ActionBreadcrumbPolicy.shouldRecord("ReopenProjectAction"))
    }

    @Test
    fun `drops missing action ids`() {
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(null))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord(""))
        assertFalse(ActionBreadcrumbPolicy.shouldRecord("   "))

        assertTrue(ActionBreadcrumbPolicy.shouldRecord("ReopenProjectAction"))
    }

    @Test
    fun `removes old action breadcrumbs and keeps the fifteen minute boundary`() {
        val now = Date(1_000_000_000)
        val breadcrumbs = listOf(
            action("old", now.time - 15 * 60 * 1000 - 1),
            action("boundary", now.time - 15 * 60 * 1000),
            action("recent", now.time - 1),
            Breadcrumb("http").apply { category = "http" },
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, now)

        assertEquals(listOf("boundary", "recent", "http"), result.map { it.message })
    }

    @Test
    fun `collapses consecutive equal actions by action and place`() {
        val now = Date(1_000_000_000)
        val breadcrumbs = listOf(
            action("GotoDeclaration", now.time, "Editor"),
            action("GotoDeclaration", now.time + 1, "Editor"),
            action("GotoDeclaration", now.time + 2, "Editor"),
            action("GotoDeclaration", now.time + 3, "Project"),
            action("SearchEverywhere", now.time + 4, "Project"),
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, Date(now.time + 5))

        assertEquals(3, result.size)
        assertEquals("GotoDeclaration", result[0].message)
        assertEquals("Editor", result[0].getData("place"))
        assertEquals(3, result[0].getData("count"))
        assertNull(result[1].getData("count"))
        assertEquals("Project", result[1].getData("place"))
        assertEquals("SearchEverywhere", result[2].message)
    }

    @Test
    fun `filters noise before collapsing useful actions`() {
        val now = Date(1_000_000_000)
        val breadcrumbs = listOf(
            action("GotoDeclaration", now.time),
            action("EditorBackSpace", now.time + 1),
            action("GotoDeclaration", now.time + 2),
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, Date(now.time + 3))

        assertEquals(1, result.size)
        assertEquals(2, result.single().getData("count"))
    }

    private fun action(message: String, timestamp: Long, place: String = "Editor") =
        Breadcrumb(Date(timestamp)).apply {
            category = "action"
            this.message = message
            level = io.sentry.SentryLevel.INFO
            setData("place", place)
        }
}
