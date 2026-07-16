/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.explyt.spring.core.statistic.FeedbackNudgeState.NudgeData
import com.intellij.openapi.components.*

/**
 * Durable, application-level persistence for the feedback nudge.
 *
 * Stored in a regular settings file (not a cache) so that "never show twice" and a permanent
 * "Dismiss" survive IDE restarts and cache invalidation.
 */
@Service(Service.Level.APP)
@State(
    name = "ExplytSpringFeedbackNudge",
    category = SettingsCategory.PLUGINS,
    storages = [Storage("explyt-spring-feedback.xml")]
)
class FeedbackNudgeState : SimplePersistentStateComponent<NudgeData>(NudgeData()) {

    class NudgeData : BaseState() {
        /** Total tracked Explyt action usages observed for this installation. */
        var totalUsages by property(0)

        /** Number of distinct calendar days on which the plugin was used. */
        var distinctActiveDays by property(0)

        /** Epoch day of the most recent recorded usage (used to count distinct days). */
        var lastActiveEpochDay by property(0L)

        /** Set once the nudge has been shown — it is never shown again. */
        var nudgeShown by property(false)

        /** Set when the user explicitly dismisses the nudge — honored forever. */
        var dismissed by property(false)
    }

    fun toStats(): NudgeStats = with(state) {
        NudgeStats(
            totalUsages = totalUsages,
            distinctActiveDays = distinctActiveDays,
            nudgeShown = nudgeShown,
            dismissed = dismissed,
        )
    }

    companion object {
        fun getInstance(): FeedbackNudgeState = service()
    }
}
