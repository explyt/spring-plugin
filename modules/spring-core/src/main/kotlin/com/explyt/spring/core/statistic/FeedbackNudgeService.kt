/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.notifications.SpringFeedbackNotificationGroup
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.IdeFocusManager
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Shows a one-time, sticky notification that invites engaged users to rate the plugin on the Marketplace or star
 * it on GitHub — directly targeting the very low rating-vote conversion.
 *
 * Engagement is measured with [FeedbackNudgeState] (local counters only — nothing leaves the machine) and gated by
 * the pure [shouldShowFeedbackNudge] decision. The notification is shown at most once ever, right after the
 * threshold is crossed; the project-startup check only covers a threshold crossed while no project was open.
 * "Don't show again" suppresses it permanently. Impressions and clicks are counted as [StatisticActionId]s.
 */
@Service(Service.Level.APP)
class FeedbackNudgeService {

    private val logger = Logger.getInstance(FeedbackNudgeService::class.java)

    /**
     * Records one unit of engagement for a real plugin action and shows the nudge as soon as the thresholds are
     * met. The nudge's own events never count (see [isEngagementAction]).
     */
    fun recordEngagement(actionId: StatisticActionId) {
        if (skipForUnitTestAndHeadlessMode()) return
        if (!isEngagementAction(actionId)) return
        try {
            val nudgeState = FeedbackNudgeState.getInstance()
            val thresholdMet = synchronized(this) {
                seedFromLegacyUsageOnce(nudgeState)
                val state = nudgeState.state
                state.totalUsages += 1
                val today = LocalDate.now().toEpochDay()
                if (state.lastActiveEpochDay != today) {
                    state.lastActiveEpochDay = today
                    state.distinctActiveDays += 1
                }
                shouldShowFeedbackNudge(nudgeState.toStats())
            }
            if (thresholdMet) showNudgeOnEdt(preferredProject = null, force = false)
        } catch (e: CancellationException) {
            // Also covers ProcessCanceledException: cancellation must never be logged or swallowed.
            throw e
        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    /**
     * Startup fallback for a threshold crossed while no project could host a notification, and the entry point of
     * the [DEBUG_REGISTRY_KEY] registry key, which forces the nudge regardless of state.
     */
    fun maybeShowNudge(project: Project) {
        if (skipForUnitTestAndHeadlessMode()) return
        try {
            val nudgeState = FeedbackNudgeState.getInstance()
            synchronized(this) { seedFromLegacyUsageOnce(nudgeState) }
            val force = Registry.`is`(DEBUG_REGISTRY_KEY, false)
            if (!force && !shouldShowFeedbackNudge(nudgeState.toStats())) return
            showNudgeOnEdt(preferredProject = project, force = force)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e)
        }
    }

    /** Folds the pre-nudge lifetime usage into the nudge state, exactly once per installation. Call under the lock. */
    private fun seedFromLegacyUsageOnce(nudgeState: FeedbackNudgeState) {
        val state = nudgeState.state
        if (state.legacySeeded) return
        val legacyTotal = synchronized(StatisticService::class.java) {
            service<StatisticState>().state.counterUsagesMap.values.sum()
        }
        val seeded = seedFromLegacyUsage(nudgeState.toStats(), legacyTotal)
        state.totalUsages = seeded.totalUsages
        state.distinctActiveDays = seeded.distinctActiveDays
        state.legacySeeded = true
    }

    /**
     * Shows the notification on the EDT outside modal dialogs. The decision is re-checked there: the EDT is serial,
     * so two engagement events racing for the same threshold cannot both show it, and `nudgeShown` is set only after
     * the notification was actually displayed. A project in dumb mode is skipped — the next engagement retries.
     */
    private fun showNudgeOnEdt(preferredProject: Project?, force: Boolean) {
        ApplicationManager.getApplication().invokeLater({
            val project = preferredProject?.takeIf { !it.isDisposed } ?: focusedOpenProject() ?: return@invokeLater
            if (DumbService.isDumb(project)) return@invokeLater
            val nudgeState = FeedbackNudgeState.getInstance()
            if (!force && !shouldShowFeedbackNudge(nudgeState.toStats())) return@invokeLater
            showNotification(project)
            synchronized(this) { nudgeState.state.nudgeShown = true }
            StatisticService.getInstance().addActionUsage(StatisticActionId.FEEDBACK_NUDGE_SHOWN)
        }, ModalityState.nonModal())
    }

    private fun focusedOpenProject(): Project? =
        IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project?.takeIf { !it.isDisposed && !it.isDefault }
            ?: ProjectManager.getInstance().openProjects.firstOrNull { !it.isDisposed }

    private fun showNotification(project: Project) {
        SpringFeedbackNotificationGroup
            .createNotification(
                message("explyt.spring.feedback.nudge.title"),
                message("explyt.spring.feedback.nudge.content"),
                NotificationType.INFORMATION
            )
            .setIcon(AllIcons.Nodes.Favorite)
            .addAction(NotificationAction.createSimpleExpiring(message("explyt.spring.feedback.nudge.rate")) {
                StatisticService.getInstance().addActionUsage(StatisticActionId.FEEDBACK_NUDGE_RATE_CLICKED)
                BrowserUtil.browse(MARKETPLACE_REVIEWS_URL)
            })
            .addAction(NotificationAction.createSimpleExpiring(message("explyt.spring.feedback.nudge.star")) {
                StatisticService.getInstance().addActionUsage(StatisticActionId.FEEDBACK_NUDGE_STAR_CLICKED)
                BrowserUtil.browse(GITHUB_REPO_URL)
            })
            .addAction(NotificationAction.createSimpleExpiring(message("explyt.spring.feedback.nudge.dismiss")) {
                StatisticService.getInstance().addActionUsage(StatisticActionId.FEEDBACK_NUDGE_DISMISSED)
                synchronized(this@FeedbackNudgeService) {
                    FeedbackNudgeState.getInstance().state.dismissed = true
                }
            })
            .notify(project)
    }

    private fun skipForUnitTestAndHeadlessMode(): Boolean =
        ApplicationManager.getApplication().isUnitTestMode ||
            ApplicationManager.getApplication().isHeadlessEnvironment

    companion object {
        fun getInstance(): FeedbackNudgeService = service()

        const val DEBUG_REGISTRY_KEY = "explyt.spring.feedback.nudge.debug"

        /** Plain destinations until the first-party, per-placement short links exist. */
        const val MARKETPLACE_REVIEWS_URL = "https://plugins.jetbrains.com/plugin/28675-spring-explyt/reviews"
        const val GITHUB_REPO_URL = "https://github.com/explyt/spring-plugin"
    }
}
