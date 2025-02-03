/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.runconfiguration

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Property

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
    var sqlLanguageId by string("")

    @get:Property(surroundWithTag = true)
    var httpCliPath by string("")

    override fun getState(): SpringToolRunConfigurationsSettingsState = this

    override fun loadState(state: SpringToolRunConfigurationsSettingsState) {
        this.copyFrom(state)
    }

    companion object {
        fun getInstance(): SpringToolRunConfigurationsSettingsState = service()
    }
}