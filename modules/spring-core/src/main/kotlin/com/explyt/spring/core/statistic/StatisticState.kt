/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.explyt.spring.core.statistic.StatisticState.UsagesMapStatistic
import com.intellij.openapi.components.*
import com.intellij.util.ThreeState

/**
 * Local usage counters.
 *
 * `useSaveThreshold = NO` opts out of the platform's rule that saves a non-roamable component at most once every five
 * minutes and does not force it on exit: with the default, every IDE session shorter than five minutes after the first
 * counted action lost its counters.
 */
@Service(Service.Level.APP)
@State(
    name = "ExplytSpringStatisticCache",
    category = SettingsCategory.PLUGINS,
    storages = [Storage(StoragePathMacros.CACHE_FILE, useSaveThreshold = ThreeState.NO)]
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