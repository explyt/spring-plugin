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

/**
 * Records useful, registered IDE actions as Sentry breadcrumbs.
 * High-frequency editor input is omitted before it reaches Sentry, so the trail keeps navigation and product actions
 * instead of being consumed by typing noise.
 */
class ActionBreadcrumbListener : AnActionListener {

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        val actionId = ActionManager.getInstance().getId(action)
        if (!ActionBreadcrumbPolicy.shouldRecord(actionId)) return

        SentryReporter.addBreadcrumb(Breadcrumb().apply {
            category = ActionBreadcrumbSanitizer.ACTION_CATEGORY
            message = actionId
            level = SentryLevel.INFO
            setData("place", event.place)
        })
    }
}
