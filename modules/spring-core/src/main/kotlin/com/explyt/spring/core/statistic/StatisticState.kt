/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.explyt.spring.core.statistic.StatisticState.UsagesMapStatistic
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "ExplytSpringStatisticCache",
    category = SettingsCategory.PLUGINS,
    storages = [Storage(StoragePathMacros.CACHE_FILE)]
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