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
 * Records each performed IDE action as a Sentry breadcrumb.
 * When an error is captured, the last 100 breadcrumbs (Sentry default) are sent
 * automatically, providing a reproduction path instead of a single random "last action".
 */
class ActionBreadcrumbListener : AnActionListener {

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        val actionId = ActionManager.getInstance().getId(action) ?: action.javaClass.simpleName

        SentryReporter.addBreadcrumb(Breadcrumb().apply {
            category = "action"
            message = actionId
            level = SentryLevel.INFO
            setData("place", event.place)
        })
    }
}
