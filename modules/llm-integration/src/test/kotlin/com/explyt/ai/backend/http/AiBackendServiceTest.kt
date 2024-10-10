package com.explyt.ai.backend.http

import com.explyt.ai.backend.backend.AiBackendLocal
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class AiBackendServiceTest {
    @Test
    fun `Smoke test of DeepSeek API`() {
        val aiBackendLocal = AiBackendLocal()
        val modelInfos = aiBackendLocal.availableModels().providerToModelConfigs[Provider.OpenAI]!!
        val response =
            runBlocking {
                aiBackendLocal.chat(
                    ChatRequest(
                        ModelConfig(
                            modelInfos[0], "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"
                        ),
                        Prompt(
                            listOf(Message.user("What is 2 + 2? Print only the answer and nothing else."))
                        )
                    )
                )
            }
        Assert.assertTrue(response.usage.price >= 0.0)
        //assertContains(response.response, "4")
    }

}
