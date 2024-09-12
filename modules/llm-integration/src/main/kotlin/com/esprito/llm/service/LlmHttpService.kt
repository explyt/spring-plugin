/*
package com.esprito.llm.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import ee.carlrobert.llm.client.codegpt.CodeGPTClient
import ee.carlrobert.llm.client.openai.completion.request.*
import ee.carlrobert.llm.completion.CompletionEventListener
import okhttp3.OkHttpClient
import okhttp3.sse.EventSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension

private const val ROLE_USER = "user"

private const val CONNECT_TIMEOUT = 10L
private const val READ_TIMEOUT = 10L

@Service(Service.Level.APP)
class LlmHttpService {
    private val freeClient: CodeGPTClient

    init {
        freeClient = CodeGPTClient(null, getDefaultClientBuilder())
    }

    companion object {
        fun getInstance(): LlmHttpService = ApplicationManager.getApplication().service()
    }

    fun freeRequestAsync(message: String, imagePath: Path?, listener: CompletionEventListener<String>): EventSource {
        val request = OpenAIChatCompletionRequest.Builder(createAIMessage(message, imagePath))
            .setModel("llama-3-8b")
            // .setMaxTokens(1000)
            .setTemperature(0.1)
            .build()

        return freeClient.getChatCompletionAsync(request, listener)
    }

    private fun createAIMessage(
        message: String, imagePath: Path?
    ): List<OpenAIChatCompletionMessage> {
        if (imagePath == null) {
            val standardMessage = OpenAIChatCompletionStandardMessage(ROLE_USER, message)
            return listOf(standardMessage)
        } else {
            val imageData = Files.readAllBytes(imagePath)
            val imageMediaType = getImageMediaType(imagePath)
            return listOf(
                OpenAIChatCompletionDetailedMessage(
                    ROLE_USER,
                    listOf(
                        OpenAIMessageImageURLContent(OpenAIImageUrl(imageMediaType, imageData)),
                        OpenAIMessageTextContent(message)
                    )
                )
            )
        }
    }

    private fun getImageMediaType(path: Path): String {
        return when (val fileExtension = path.extension) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> throw IllegalArgumentException("Unsupported image type: $fileExtension")
        }
    }

    private fun getDefaultClientBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()

        */
/*  val proxyHost: String = settings.getProxyHost()
          val proxyPort: Int = settings.getProxyPort()
          if (proxyHost.isNotEmpty() && proxyPort != 0) {
              builder.proxy(
                  Proxy(advancedSettings.getProxyType(), InetSocketAddress(proxyHost, proxyPort))
              )
              if (advancedSettings.isProxyAuthSelected()) {
                  builder.proxyAuthenticator { route: Route?, response: Response ->
                      response.request()
                          .newBuilder()
                          .header(
                              "Proxy-Authorization", basic(
                                  advancedSettings.getProxyUsername(),
                                  advancedSettings.getProxyPassword()
                              )
                          )
                          .build()
                  }
              }
          }*//*


        return builder
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
    }
}*/
