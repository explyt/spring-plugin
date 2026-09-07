/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.explyt.spring.core.statistic.StatisticState.UsagesMapStatistic
import com.intellij.openapi.components.*

/**
 * Local usage counters.
 *
 * Stored in a plain options file with [RoamingType.LOCAL]: kept on this machine, never shared through Settings Sync
 * or exported, readable as XML. The previous `$CACHE_FILE$` storage is non-roamable, which on 2026.2 routes the state
 * into the opaque internal settings database and saves it at most once every five minutes without forcing the save
 * on exit, so every IDE session shorter than that lost its counters.
 */
@Service(Service.Level.APP)
@State(
    name = "ExplytSpringStatisticCache",
    category = SettingsCategory.PLUGINS,
    storages = [Storage("explyt-spring-statistic.xml", roamingType = RoamingType.LOCAL)]
)
class StatisticState : SimplePersistentStateComponent<UsagesMapStatistic>(UsagesMapStatistic()) {

    class UsagesMapStatistic : BaseState() {
        var counterUsagesMap by map<String, Int>()

        /**
         * Increments a usage counter through `put`.
         *
         * The platform's stored map counts only `put`, `remove` and `clear` as modifications; `Map.compute` on it
         * is implemented natively and never marks the state dirty, so a component that only ever used `compute`
         * was never saved — no usage counter survived an IDE restart until this method replaced it.
         */
        fun incrementUsage(actionName: String) {
            counterUsagesMap[actionName] = (counterUsagesMap[actionName] ?: 0) + 1
        }
    }
}