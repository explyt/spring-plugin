/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.explyt.spring.core.statistic.StatisticActionId.COMPLETION_PROPERTY_KEY_CONFIGURATION
import com.explyt.spring.core.statistic.StatisticActionId.DEVICE_ID
import com.explyt.spring.core.statistic.StatisticActionId.FEEDBACK_NUDGE_DISMISSED
import com.explyt.spring.core.statistic.StatisticActionId.FEEDBACK_NUDGE_RATE_CLICKED
import com.explyt.spring.core.statistic.StatisticActionId.FEEDBACK_NUDGE_SHOWN
import com.explyt.spring.core.statistic.StatisticActionId.FEEDBACK_NUDGE_STAR_CLICKED
import com.explyt.spring.core.statistic.StatisticActionId.GUTTER_BEAN_USAGE
import com.explyt.spring.core.statistic.StatisticActionId.LOCAL_DATE
import com.explyt.spring.core.statistic.StatisticActionId.PLUGIN_VERSION
import org.junit.Assert.assertEquals
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

    // --- Seeding from the legacy StatisticState counters (lifetime usage of installs that predate the nudge) ---

    @Test
    fun seedingLeavesAFreshInstallUntouched() {
        val seeded = seedFromLegacyUsage(stats(totalUsages = 0, distinctActiveDays = 0), legacyTotalUsages = 0)
        assertEquals(0, seeded.totalUsages)
        assertEquals(0, seeded.distinctActiveDays)
    }

    @Test
    fun seedingNeverLowersAlreadyRecordedUsage() {
        val seeded = seedFromLegacyUsage(stats(totalUsages = 100, distinctActiveDays = 1), legacyTotalUsages = 40)
        assertEquals(100, seeded.totalUsages)
        assertEquals(1, seeded.distinctActiveDays)
    }

    @Test
    fun moderateLegacyUsageSeedsTheCounterButStillWaitsForActiveDays() {
        val legacy = LEGACY_USAGES_FOR_INSTANT_QUALIFICATION - 1
        val seeded = seedFromLegacyUsage(stats(totalUsages = 0, distinctActiveDays = 0), legacyTotalUsages = legacy)
        assertEquals(legacy, seeded.totalUsages)
        assertEquals(0, seeded.distinctActiveDays)
        assertFalse(shouldShowFeedbackNudge(seeded))
    }

    @Test
    fun heavyLegacyUsageQualifiesImmediately() {
        val seeded = seedFromLegacyUsage(
            stats(totalUsages = 0, distinctActiveDays = 0),
            legacyTotalUsages = LEGACY_USAGES_FOR_INSTANT_QUALIFICATION,
        )
        assertTrue(shouldShowFeedbackNudge(seeded))
    }

    @Test
    fun seedingPreservesDismissalAndShownFlags() {
        val dismissed = seedFromLegacyUsage(stats(dismissed = true), legacyTotalUsages = 10_000)
        assertTrue(dismissed.dismissed)
        assertFalse(shouldShowFeedbackNudge(dismissed))

        val shown = seedFromLegacyUsage(stats(nudgeShown = true), legacyTotalUsages = 10_000)
        assertTrue(shown.nudgeShown)
        assertFalse(shouldShowFeedbackNudge(shown))
    }

    // --- The nudge's own events must never feed the engagement counter ---

    @Test
    fun nudgeEventsAreNotEngagement() {
        for (id in listOf(
            FEEDBACK_NUDGE_SHOWN, FEEDBACK_NUDGE_RATE_CLICKED, FEEDBACK_NUDGE_STAR_CLICKED, FEEDBACK_NUDGE_DISMISSED,
        )) {
            assertFalse(id.name, isEngagementAction(id))
        }
    }

    @Test
    fun statisticMetadataIdsAreNotEngagement() {
        for (id in listOf(DEVICE_ID, PLUGIN_VERSION, LOCAL_DATE)) {
            assertFalse(id.name, isEngagementAction(id))
        }
    }

    @Test
    fun regularPluginActionsAreEngagement() {
        assertTrue(isEngagementAction(GUTTER_BEAN_USAGE))
        assertTrue(isEngagementAction(COMPLETION_PROPERTY_KEY_CONFIGURATION))
    }
}
