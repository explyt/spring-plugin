/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Property
import java.time.Instant

@Service(Service.Level.APP)
@State(
    name = "SpringToolRunConfigurations",
    category = SettingsCategory.PLUGINS,
    defaultStateAsResource = true,
    storages = [Storage(value = StoragePathMacros.NON_ROAMABLE_FILE)]
)
class SpringToolRunConfigurationsSettingsState :
    PersistentStateComponent<SpringToolRunConfigurationsSettingsState>, BaseState() {


    @get:Property(surroundWithTag = true)
    var isAutoDetectConfigurations by property(true)

    @get:Property(surroundWithTag = true)
    var isBeanFilterEnabled by property(true)

    @get:Property(surroundWithTag = true)
    var isCollectStatistic by property(true)

    @get:Property(surroundWithTag = true)
    var isShowFloatingRefreshAction by property(true)

    @get:Property(surroundWithTag = true)
    var isDebugMode by property(true)

    @get:Property(surroundWithTag = true)
    var isJavaAgentMode by property(true)

    @get:Property(surroundWithTag = true)
    var sqlLanguageId by string("")

    @get:Property(surroundWithTag = true)
    var httpCliPath by string("")

    @get:Property(surroundWithTag = true)
    var aiSuggestInstantSecond by property(Instant.MIN.epochSecond)

    override fun getState(): SpringToolRunConfigurationsSettingsState = this

    override fun loadState(state: SpringToolRunConfigurationsSettingsState) {
        this.copyFrom(state)
    }

    companion object {
        fun getInstance(): SpringToolRunConfigurationsSettingsState = service()
    }
}