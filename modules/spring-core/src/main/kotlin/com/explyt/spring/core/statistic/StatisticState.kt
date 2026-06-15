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
    }
}