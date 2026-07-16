/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackNudgeDecisionTest {

    private fun stats(
        totalUsages: Int = MIN_TOTAL_USAGES_FOR_NUDGE,
        distinctActiveDays: Int = MIN_ACTIVE_DAYS_FOR_NUDGE,
        nudgeShown: Boolean = false,
        dismissed: Boolean = false,
    ) = NudgeStats(totalUsages, distinctActiveDays, nudgeShown, dismissed)

    @Test
    fun showsWhenThresholdsMet() {
        assertTrue(shouldShowFeedbackNudge(stats()))
    }

    @Test
    fun showsWhenWellAboveThresholds() {
        assertTrue(shouldShowFeedbackNudge(stats(totalUsages = 500, distinctActiveDays = 42)))
    }

    @Test
    fun hiddenWhenNotEnoughUsages() {
        assertFalse(shouldShowFeedbackNudge(stats(totalUsages = MIN_TOTAL_USAGES_FOR_NUDGE - 1)))
    }

    @Test
    fun hiddenWhenNotEnoughActiveDays() {
        assertFalse(shouldShowFeedbackNudge(stats(distinctActiveDays = MIN_ACTIVE_DAYS_FOR_NUDGE - 1)))
    }

    @Test
    fun hiddenWhenAlreadyShown() {
        assertFalse(shouldShowFeedbackNudge(stats(nudgeShown = true)))
    }

    @Test
    fun hiddenWhenDismissed() {
        assertFalse(shouldShowFeedbackNudge(stats(dismissed = true)))
    }

    @Test
    fun dismissedTakesPrecedenceOverHighEngagement() {
        assertFalse(shouldShowFeedbackNudge(stats(totalUsages = 9999, distinctActiveDays = 99, dismissed = true)))
    }

    @Test
    fun hiddenAtZeroEngagement() {
        assertFalse(shouldShowFeedbackNudge(stats(totalUsages = 0, distinctActiveDays = 0)))
    }
}
