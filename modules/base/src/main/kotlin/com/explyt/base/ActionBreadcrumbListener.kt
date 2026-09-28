/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import io.sentry.Breadcrumb
import io.sentry.SentryLevel
import java.util.Date

/**
 * Records useful, registered IDE actions as Sentry breadcrumbs.
 * High-frequency editor input is omitted before it reaches Sentry, so the trail keeps navigation and product actions
 * instead of being consumed by typing noise.
 */
class ActionBreadcrumbListener : AnActionListener {

    private val repeatedActions = RepeatedActionSuppressor()

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        val actionId = ActionManager.getInstance().getId(action)
        val now = Date()
        val name = ActionBreadcrumbPolicy.breadcrumbName(actionId, action.javaClass)
        if (name == null) {
            repeatedActions.breakRun()?.let(::recordRepeats)
            return
        }

        val decision = repeatedActions.onAction(name, event.place, now)
        decision.endedRun?.let(::recordRepeats)
        if (decision.record) record(name, event.place, now, count = null)
    }

    /** Recorded with the time of the last repeat, so the collapsed breadcrumb is timed by its newest occurrence. */
    private fun recordRepeats(repeats: RepeatedActionSuppressor.SuppressedRepeats) =
        record(repeats.actionName, repeats.place, repeats.lastSeen, repeats.count)

    private fun record(name: String, place: String?, at: Date, count: Int?) {
        SentryReporter.addBreadcrumb(Breadcrumb(at).apply {
            category = ActionBreadcrumbSanitizer.ACTION_CATEGORY
            message = name
            level = SentryLevel.INFO
            place?.let { setData(ActionBreadcrumbSanitizer.PLACE_KEY, it) }
            count?.let { setData(ActionBreadcrumbSanitizer.COUNT_KEY, it) }
        })
    }
}
