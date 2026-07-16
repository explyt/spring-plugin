/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

/**
 * Engagement thresholds that gate the one-time feedback nudge.
 *
 * The nudge is shown only to genuinely engaged users (proven by real usage spread across
 * several days), which keeps review prompts effective instead of rating-poisoning.
 */
const val MIN_TOTAL_USAGES_FOR_NUDGE: Int = 30
const val MIN_ACTIVE_DAYS_FOR_NUDGE: Int = 3

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
