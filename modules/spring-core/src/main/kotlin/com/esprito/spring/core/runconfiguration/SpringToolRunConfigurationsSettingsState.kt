package com.esprito.spring.core.runconfiguration

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

    override fun getState(): SpringToolRunConfigurationsSettingsState = this

    override fun loadState(state: SpringToolRunConfigurationsSettingsState) {
        this.copyFrom(state)
    }

    companion object {
        fun getInstance(): SpringToolRunConfigurationsSettingsState = service()
    }
}