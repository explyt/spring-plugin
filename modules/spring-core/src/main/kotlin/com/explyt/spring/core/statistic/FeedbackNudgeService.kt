/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.notifications.SpringToolNotificationGroup
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import java.time.LocalDate

/**
 * Shows a one-time, non-intrusive notification that invites engaged users to rate the plugin on
 * the Marketplace or star it on GitHub — directly targeting the very low rating-vote conversion.
 *
 * Engagement is measured with [FeedbackNudgeState] (local counters only — no telemetry, no network)
 * and gated by the pure [shouldShowFeedbackNudge] decision. The balloon is shown at most once ever,
 * and "Dismiss" suppresses it permanently.
 */
@Service(Service.Level.APP)
class FeedbackNudgeService {

    private val logger = Logger.getInstance(FeedbackNudgeService::class.java)

    /**
     * Records a single unit of engagement: increments the total usage counter and, the first time
     * the plugin is used on a new calendar day, the distinct-active-days counter.
     */
    fun recordEngagement() {
        if (skipForUnitTestAndHeadlessMode()) return
        try {
            val state = FeedbackNudgeState.getInstance().state
            synchronized(this) {
                state.totalUsages += 1
                val today = LocalDate.now().toEpochDay()
                if (state.lastActiveEpochDay != today) {
                    state.lastActiveEpochDay = today
                    state.distinctActiveDays += 1
                }
            }
        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    /** Evaluates the thresholds and, if met, shows the nudge exactly once. */
    fun maybeShowNudge(project: Project) {
        if (skipForUnitTestAndHeadlessMode()) return
        try {
            val nudgeState = FeedbackNudgeState.getInstance()
            val forceShow = Registry.`is`("explyt.spring.feedback.nudge.debug", false)
            if (!forceShow && !shouldShowFeedbackNudge(nudgeState.toStats())) return

            // Mark as shown *before* displaying, so a crash/restart can never double-show it.
            nudgeState.state.nudgeShown = true
            showNotification(project)
        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    private fun showNotification(project: Project) {
        SpringToolNotificationGroup
            .createNotification(
                message("explyt.spring.feedback.nudge.title"),
                message("explyt.spring.feedback.nudge.content"),
                NotificationType.INFORMATION
            )
            .setIcon(AllIcons.Nodes.Favorite)
            .addAction(NotificationAction.createSimpleExpiring(message("explyt.spring.feedback.nudge.rate")) {
                BrowserUtil.browse(MARKETPLACE_REVIEWS_URL)
            })
            .addAction(NotificationAction.createSimpleExpiring(message("explyt.spring.feedback.nudge.star")) {
                BrowserUtil.browse(GITHUB_REPO_URL)
            })
            .addAction(NotificationAction.createSimpleExpiring(message("explyt.spring.feedback.nudge.dismiss")) {
                FeedbackNudgeState.getInstance().state.dismissed = true
            })
            .notify(project)
    }

    private fun skipForUnitTestAndHeadlessMode(): Boolean =
        ApplicationManager.getApplication().isUnitTestMode ||
            ApplicationManager.getApplication().isHeadlessEnvironment

    companion object {
        fun getInstance(): FeedbackNudgeService = service()

        const val MARKETPLACE_REVIEWS_URL = "https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews"
        const val GITHUB_REPO_URL = "https://github.com/explyt/spring-plugin"
    }
}
