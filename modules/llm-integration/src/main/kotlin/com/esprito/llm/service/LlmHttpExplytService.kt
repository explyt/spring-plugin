package com.esprito.llm.service

import com.esprito.llm.model.LlmChat
import com.esprito.llm.model.LlmMessage
import com.esprito.llm.settings.LlmIntegrationSettingsState
import com.explyt.ai.backend.backend.AiBackendLocal
import com.explyt.ai.backend.http.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@Service(Service.Level.APP)
class LlmHttpExplytService {
    val aiBackendLocal = AiBackendLocal()

    companion object {
        fun getInstance(): LlmHttpExplytService = ApplicationManager.getApplication().service()
    }

    fun perform(chat: LlmChat): Deferred<ChatResponse> {
        return CoroutineScope(Dispatchers.IO).async {
            val settings = LlmIntegrationSettingsState.getInstance()
            val provider = settings.provider?.let { Provider.valueOf(it) }
                ?: throw RuntimeException("Provider no selected")
            val modelInfo = getModelInfo(provider, settings)

            val messages = chat.messages.flatMap { toLlmRequestMessages(it) }
            val modelConfig = getModelConfig(modelInfo, provider, settings)
            aiBackendLocal.chat(ChatRequest(modelConfig, Prompt(messages)))
        }
    }

    private fun getModelConfig(
        modelInfo: ModelInfo, provider: Provider, settings: LlmIntegrationSettingsState
    ): ModelConfig {
        return if (provider == Provider.Ollama) ModelConfig(modelInfo) else ModelConfig(modelInfo, settings.apiKey)
    }

    private suspend fun getModelInfo(provider: Provider, settings: LlmIntegrationSettingsState): ModelInfo {
        val models = if (provider == Provider.Ollama) {
            val ollamaServerUrl = settings.ollamaUrl?.takeIf { it.isNotBlank() }
                ?: throw RuntimeException("Empty ollama url")
            aiBackendLocal.availableOllamaModels(AvailableOllamaModelsRequest(ollamaServerUrl)).models
        } else aiBackendLocal.availableModels().providerToModelConfigs[provider]

        return models?.find { it.modelName == settings.providerModel }
            ?: throw RuntimeException("Provider $provider model no selected")
    }

    private fun toLlmRequestMessages(message: LlmMessage): List<Message> {
        val userPrompt = message.userFinalPrompt ?: return emptyList()
        val result = mutableListOf(Message.user(userPrompt))
        message.response?.also { result.add(Message.assistant(it)) }
        return result
    }
}