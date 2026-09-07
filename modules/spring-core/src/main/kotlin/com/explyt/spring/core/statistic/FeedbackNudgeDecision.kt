/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import kotlin.math.max

/**
 * Engagement thresholds that gate the one-time feedback nudge.
 *
 * The nudge is shown only to genuinely engaged users (proven by real usage spread across
 * several days), which keeps review prompts effective instead of rating-poisoning.
 */
const val MIN_TOTAL_USAGES_FOR_NUDGE: Int = 30
const val MIN_ACTIVE_DAYS_FOR_NUDGE: Int = 3

/**
 * Lifetime usage, taken from the legacy [StatisticState] counters, at which an install that predates the nudge
 * counts as engaged without waiting for [MIN_ACTIVE_DAYS_FOR_NUDGE] more days: three times the usage threshold
 * cannot plausibly come from a single sitting.
 */
const val LEGACY_USAGES_FOR_INSTANT_QUALIFICATION: Int = 3 * MIN_TOTAL_USAGES_FOR_NUDGE

/** Immutable snapshot of the persisted engagement state, used by [shouldShowFeedbackNudge]. */
data class NudgeStats(
    val totalUsages: Int,
    val distinctActiveDays: Int,
    val nudgeShown: Boolean,
    val dismissed: Boolean,
)

/**
 * Pure decision function: returns `true` when the feedback nudge should be shown.
 *
 * Kept free of any IntelliJ-platform dependency so it can be unit-tested in isolation.
 */
fun shouldShowFeedbackNudge(stats: NudgeStats): Boolean =
    !stats.nudgeShown &&
        !stats.dismissed &&
        stats.totalUsages >= MIN_TOTAL_USAGES_FOR_NUDGE &&
        stats.distinctActiveDays >= MIN_ACTIVE_DAYS_FOR_NUDGE

/**
 * Folds the usage an install accumulated before the nudge existed into [stats].
 *
 * The counter is never lowered. Heavy legacy usage ([LEGACY_USAGES_FOR_INSTANT_QUALIFICATION] or more) also
 * satisfies the active-days rule, because the legacy counters carry no per-day information and such a user has
 * evidently been around for a while. The shown and dismissed flags are left untouched.
 */
fun seedFromLegacyUsage(stats: NudgeStats, legacyTotalUsages: Int): NudgeStats {
    val qualifiesInstantly = legacyTotalUsages >= LEGACY_USAGES_FOR_INSTANT_QUALIFICATION
    return stats.copy(
        totalUsages = max(stats.totalUsages, legacyTotalUsages),
        distinctActiveDays = if (qualifiesInstantly) {
            max(stats.distinctActiveDays, MIN_ACTIVE_DAYS_FOR_NUDGE)
        } else {
            stats.distinctActiveDays
        },
    )
}

/** The nudge's own events and the statistics-file metadata keys: neither may ever count as engagement. */
private val NON_ENGAGEMENT_ACTIONS: Set<StatisticActionId> = setOf(
    StatisticActionId.DEVICE_ID,
    StatisticActionId.PLUGIN_VERSION,
    StatisticActionId.LOCAL_DATE,
    StatisticActionId.FEEDBACK_NUDGE_SHOWN,
    StatisticActionId.FEEDBACK_NUDGE_RATE_CLICKED,
    StatisticActionId.FEEDBACK_NUDGE_STAR_CLICKED,
    StatisticActionId.FEEDBACK_NUDGE_DISMISSED,
)

/** `true` for a real plugin action; `false` for the nudge's own events and for statistics metadata. */
fun isEngagementAction(actionId: StatisticActionId): Boolean = actionId !in NON_ENGAGEMENT_ACTIONS
