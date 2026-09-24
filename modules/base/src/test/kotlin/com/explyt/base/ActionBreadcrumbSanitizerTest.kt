/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import io.sentry.Breadcrumb
import io.sentry.SentryLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class ActionBreadcrumbSanitizerTest {

    /**
     * The cutoff is measured from the event, not from the moment of sending: a user can submit a report hours after
     * the failure, and measuring from "now" would erase the whole trail — including the single entry that explains it.
     */
    @Test
    fun `keeps the trail of an event reported much later`() {
        val eventTime = Date(DAY)
        val breadcrumbs = listOf(action("ReopenProjectAction", eventTime.time - MINUTE))

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime)

        assertEquals(listOf("ReopenProjectAction"), result.map { it.message })
    }

    @Test
    fun `removes entries older than the boundary relative to the event`() {
        val eventTime = Date(DAY)
        val breadcrumbs = listOf(
            action("stale", eventTime.time - 15 * MINUTE - 1),
            action("boundary", eventTime.time - 15 * MINUTE),
            action("recent", eventTime.time - 1),
            Breadcrumb("http").apply { category = "http" },
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime)

        assertEquals(listOf("boundary", "recent", "http"), result.map { it.message })
    }

    /** A trail must never be emptied by the age filter alone: the newest actions are the reproduction path. */
    @Test
    fun `keeps the newest actions when every one of them is stale`() {
        val eventTime = Date(DAY)
        val breadcrumbs = (1..12).map { action("Action$it", eventTime.time - (60 - it) * MINUTE) }

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime)

        assertEquals(
            listOf(
                "Action3", "Action4", "Action5", "Action6", "Action7",
                "Action8", "Action9", "Action10", "Action11", "Action12",
            ),
            result.map { it.message },
        )
    }

    @Test
    fun `collapses consecutive equal actions by action and place`() {
        val eventTime = Date(DAY)
        val breadcrumbs = listOf(
            action("GotoDeclaration", eventTime.time - 4, "Editor"),
            action("GotoDeclaration", eventTime.time - 3, "Editor"),
            action("GotoDeclaration", eventTime.time - 2, "Editor"),
            action("GotoDeclaration", eventTime.time - 1, "Project"),
            action("SearchEverywhere", eventTime.time, "Project"),
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime)

        assertEquals(3, result.size)
        assertEquals("GotoDeclaration", result[0].message)
        assertEquals("Editor", result[0].getData("place"))
        assertEquals(3, result[0].getData("count"))
        assertNull(result[1].getData("count"))
        assertEquals("Project", result[1].getData("place"))
        assertEquals("SearchEverywhere", result[2].message)
    }

    /** A collapsed group must be timed by its last occurrence, otherwise "the last action" is shown too early. */
    @Test
    fun `collapsed group reports the newest timestamp and keeps the first one`() {
        val eventTime = Date(DAY)
        val first = eventTime.time - 3 * MINUTE
        val last = eventTime.time - MINUTE
        val breadcrumbs = listOf(
            action("GotoDeclaration", first),
            action("GotoDeclaration", eventTime.time - 2 * MINUTE),
            action("GotoDeclaration", last),
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime).single()

        assertEquals(Date(last), result.timestamp)
        assertEquals(3, result.getData("count"))
        assertEquals(Date(first).toString(), result.getData("first_seen"))
    }

    @Test
    fun `carries every data key into a collapsed group`() {
        val eventTime = Date(DAY)
        val breadcrumbs = listOf(
            action("GotoDeclaration", eventTime.time - 1).apply { setData("custom", "kept") },
            action("GotoDeclaration", eventTime.time).apply { setData("custom", "kept") },
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime).single()

        assertEquals("kept", result.getData("custom"))
        assertEquals("Editor", result.getData("place"))
    }

    @Test
    fun `filters noise before collapsing useful actions`() {
        val eventTime = Date(DAY)
        val breadcrumbs = listOf(
            action("GotoDeclaration", eventTime.time - 2),
            action("EditorBackSpace", eventTime.time - 1),
            action("GotoDeclaration", eventTime.time),
        )

        val result = ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime)

        assertEquals(1, result.size)
        assertEquals(2, result.single().getData("count"))
    }

    /** Sentry serializes the global scope concurrently, so the input breadcrumbs must never be mutated. */
    @Test
    fun `does not mutate the input breadcrumbs`() {
        val eventTime = Date(DAY)
        val repeated = action("GotoDeclaration", eventTime.time - 1)
        val breadcrumbs = listOf(repeated, action("GotoDeclaration", eventTime.time))

        ActionBreadcrumbSanitizer.sanitize(breadcrumbs, eventTime)

        assertNull("the source breadcrumb must not gain a count", repeated.getData("count"))
        assertEquals(2, breadcrumbs.size)
    }

    /** Sanitizing must not fail the whole report; the original trail survives an unexpected error. */
    @Test
    fun `returns the original trail when an entry cannot be processed`() {
        val eventTime = Date(DAY)
        val breadcrumbs = listOf(action("GotoDeclaration", eventTime.time))

        val result = ActionBreadcrumbSanitizer.sanitizeSafely(breadcrumbs, eventTime)

        assertEquals(listOf("GotoDeclaration"), result.map { it.message })
    }

    private fun action(message: String, timestamp: Long, place: String = "Editor") =
        Breadcrumb(Date(timestamp)).apply {
            category = ActionBreadcrumbSanitizer.ACTION_CATEGORY
            this.message = message
            level = SentryLevel.INFO
            setData("place", place)
        }

    private companion object {
        private const val MINUTE = 60 * 1000L
        private const val DAY = 24 * 60 * MINUTE
    }
}
