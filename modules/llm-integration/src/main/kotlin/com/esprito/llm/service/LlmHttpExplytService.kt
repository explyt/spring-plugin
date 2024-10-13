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
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.APP)
class LlmHttpExplytService {
    val aiBackendLocal = AiBackendLocal()

    fun perform(chat: LlmChat): Deferred<ChatResponse> {
        return CoroutineScope(Dispatchers.IO).async {
            val settings = LlmIntegrationSettingsState.getInstance()
            val provider = settings.provider?.let { Provider.valueOf(it) }
                ?: throw LlmConfigurationException("Provider no selected")
            val modelInfo = getModelInfo(provider, settings)

            val messages = getAllContextMessages(chat)
            val modelConfig = getModelConfig(modelInfo, provider, settings)
            aiBackendLocal.chat(ChatRequest(modelConfig, Prompt(messages)))
        }
    }

    private fun getModelConfig(
        modelInfo: ModelInfo, provider: Provider, settings: LlmIntegrationSettingsState
    ): ModelConfig {
        return if (provider == Provider.Ollama) ModelConfig(modelInfo) else {
            val apiKey = settings.apiKey
            if (apiKey.isNullOrEmpty()) throw LlmConfigurationException("No api key")
            ModelConfig(modelInfo, apiKey)
        }
    }

    private suspend fun getModelInfo(provider: Provider, settings: LlmIntegrationSettingsState): ModelInfo {
        val models = if (provider == Provider.Ollama) {
            val ollamaServerUrl = settings.ollamaUrl?.takeIf { it.isNotBlank() }
                ?: throw LlmConfigurationException("Empty ollama url")
            aiBackendLocal.availableOllamaModels(AvailableOllamaModelsRequest(ollamaServerUrl)).models
        } else aiBackendLocal.availableModels().providerToModelConfigs[provider]

        return models?.find { it.modelName == settings.providerModel }
            ?: throw LlmConfigurationException("Provider $provider model no selected")
    }


    companion object {
        fun getInstance(): LlmHttpExplytService = ApplicationManager.getApplication().service()

        @VisibleForTesting
        fun getAllContextMessages(chat: LlmChat): List<Message> {
            val messages = chat.messages.flatMap { toLlmRequestMessages(it) }
            val result = mutableListOf<Message>()
            for (message in messages) {
                if (result.isNotEmpty() && result.last().type == message.type) {
                    result[result.lastIndex] = message
                } else {
                    result.add(message)
                }
            }
            return result
        }

        private fun toLlmRequestMessages(message: LlmMessage): List<Message> {
            val userPrompt = message.userFinalPrompt ?: return emptyList()
            val result = mutableListOf(Message.user(userPrompt))
            message.response?.also { result.add(Message.assistant(it)) }
            return result
        }
    }
}

class LlmConfigurationException(message: String) : RuntimeException(message)