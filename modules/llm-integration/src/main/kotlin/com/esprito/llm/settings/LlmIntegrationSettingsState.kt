package com.esprito.llm.settings

import com.explyt.ai.backend.http.Provider
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.annotations.Property

@Service(Service.Level.APP)
@State(
    name = "LlmIntegrationSettings",
    category = SettingsCategory.PLUGINS,
    defaultStateAsResource = true,
    storages = [Storage(value = StoragePathMacros.NON_ROAMABLE_FILE)]
)
class LlmIntegrationSettingsState : PersistentStateComponent<LlmIntegrationSettingsState>, BaseState() {

    @get:Property(surroundWithTag = true)
    var provider by string(Provider.OpenAI.name)

    @get:Property(surroundWithTag = true)
    var providerModel by string("")

    @get:Property(surroundWithTag = true)
    var apiKey by string("")

    @get:Property(surroundWithTag = true)
    var ollamaUrl by string("")


    override fun getState(): LlmIntegrationSettingsState = this

    override fun loadState(state: LlmIntegrationSettingsState) {
        this.copyFrom(state)
    }

    companion object {
        fun getInstance(): LlmIntegrationSettingsState = service()
    }
}