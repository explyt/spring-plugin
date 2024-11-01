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

package com.explyt.spring.web.editor.openapi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.HttpRequests
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedStream
import org.jetbrains.ide.HttpRequestHandler
import org.jetbrains.io.send
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URLConnection


class OpenApiProxyRequestHandler : HttpRequestHandler() {

    override fun isSupported(request: FullHttpRequest): Boolean {
        if (!Registry.`is`("openapi.ui.proxy.enable")) return false

        return request.uri().startsWith(OpenApiUtils.OPENAPI_INTERNAL_CORS)
    }

    override fun process(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        val url = request.headers()[OpenApiUtils.OPENAPI_ORIGINAL_URL] ?: return false
        val timeout = Registry.intValue("openapi.ui.proxy.timeout")

        wrappingExceptionsToResponse(url, request, context) {
            HttpRequests.request(url).connectTimeout(timeout).readTimeout(timeout)
                .tuner { connection ->
                    copyRequestData(connection, request)
                }.connect { newRequest ->
                    processRequest(newRequest, request, context)
                }
        }

        return true
    }

    private fun copyRequestData(connection: URLConnection, originalRequest: FullHttpRequest) {
        if (connection !is HttpURLConnection) return

        connection.doInput = true
        connection.doOutput = true
        connection.requestMethod = originalRequest.method().name()


        originalRequest.headers().asSequence()
            .filter { it.key !in IGNORED_HEADERS }
            .groupBy { it.key }
            .map { it.key to squashHeaderValues(it.value) }
            .forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
    }

    private fun squashHeaderValues(values: List<Map.Entry<String, String>>): String {
        return values.asSequence()
            .map { it.value }
            .joinToString(",")
    }

    private fun processRequest(
        newRequest: HttpRequests.Request,
        originalRequest: FullHttpRequest,
        context: ChannelHandlerContext
    ) {
        wrappingExceptionsToResponse(newRequest.url, originalRequest, context) {
            writeRequestBody(originalRequest, newRequest)
            redirectResponse(originalRequest, newRequest, context)
        }
    }

    private fun writeRequestBody(originalRequest: FullHttpRequest, newRequest: HttpRequests.Request) {
        if (originalRequest.content().readableBytes() <= 0) return
        val connection = newRequest.connection as? HttpURLConnection ?: return

        connection.outputStream.use { outputStream ->
            ByteBufInputStream(originalRequest.content()).use { inputStream ->
                inputStream.transferTo(outputStream)
            }
        }
    }

    private fun wrappingExceptionsToResponse(
        url: String,
        originalRequest: FullHttpRequest,
        context: ChannelHandlerContext,
        unsafeAction: () -> Unit
    ) {
        val logger = Logger.getInstance(OpenApiProxyRequestHandler::class.java)

        try {
            unsafeAction.invoke()
        } catch (ex: HttpRequests.HttpStatusException) {
            logger.warn(
                "Unable to process request to '$url', received status code '${ex.statusCode}'", ex
            )
            DefaultHttpResponse(
                originalRequest.protocolVersion(),
                HttpResponseStatus.valueOf(ex.statusCode)
            )
                .send(context.channel(), originalRequest)
        } catch (ex: ConnectException) {
            logger.warn("Unable to execute request to '$url', connection refused", ex)
            DefaultHttpResponse(
                originalRequest.protocolVersion(),
                HttpResponseStatus.valueOf(502)
            )
                .send(context.channel(), originalRequest)
        } catch (ex: IOException) {
            logger.warn("Unable to execute request to '$url'", ex)
            DefaultHttpResponse(
                originalRequest.protocolVersion(),
                HttpResponseStatus.valueOf(418)
            )
                .send(context.channel(), originalRequest)
        }
    }


    private fun redirectResponse(
        originalRequest: FullHttpRequest,
        newRequest: HttpRequests.Request,
        context: ChannelHandlerContext
    ) {
        val httpURLConnection = newRequest.connection as? HttpURLConnection ?: return

        val responseCode = httpURLConnection.responseCode
        if (responseCode >= 400) throw HttpRequests.HttpStatusException("", responseCode, newRequest.url)

        val httpVersion = originalRequest.protocolVersion()
        val status = HttpResponseStatus.valueOf(responseCode)
        val headers = wrapOriginalHeaders(httpURLConnection.headerFields)

        val response = DefaultHttpResponse(httpVersion, status, headers)

        context.channel().write(response)

        newRequest.inputStream.use { inputStream ->
            context.channel().writeAndFlush(ChunkedStream(inputStream))
        }
    }

    private fun wrapOriginalHeaders(originalHeaders: Map<String?, List<String>?>): HttpHeaders {
        val httpHeaders = DefaultHttpHeaders()

        for (header in originalHeaders) {
            if (header.key == null) continue

            httpHeaders.set(header.key, header.value)
        }

        return httpHeaders
    }


    companion object {
        private val IGNORED_HEADERS = setOf(OpenApiUtils.OPENAPI_ORIGINAL_URL, "Accept-Encoding")
    }

}
