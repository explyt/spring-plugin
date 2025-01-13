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

import com.explyt.spring.web.SpringWebBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.HttpRequests.HttpStatusException
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedStream
import org.jetbrains.ide.HttpRequestHandler
import org.jetbrains.io.send
import java.io.InputStream
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.time.Duration


class OpenApiProxyRequestHandler : HttpRequestHandler() {

    override fun isSupported(request: FullHttpRequest): Boolean {
        return request.uri().startsWith(OpenApiUtils.OPENAPI_INTERNAL_CORS)
    }

    override fun process(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        val url = request.headers()[OpenApiUtils.OPENAPI_ORIGINAL_URL] ?: return false
        val timeout = Registry.intValue("openapi.ui.proxy.timeout").toLong()

        val httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(timeout))
            .build()

        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))

        val auth = request.headers().get(OpenApiUtils.OPENAPI_AUTH_HEADER, "")
        if (auth.isNotBlank()) {
            requestBuilder
                .header("Authorization", auth)
        }
        for ((name, value) in request.headers().entries()) {
            if (name.lowercase() in OpenApiUtils.headersToExclude) continue

            requestBuilder.header(name, value)
        }

        val httpRequest = requestBuilder
            .method(request.method().name(), bodyPublisherOf(request))
            .build()

        try {
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())

            val headers = mapToHeaders(response.headers().map())

            val newResponse = DefaultHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode()),
                headers
            )

            val channel = context.channel()
            channel.write(newResponse)
            channel.write(ChunkedStream(response.body()))
            if (isContentLengthOmitted(response)) {
                channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
            } else {
                channel.flush()
            }
            channel.close()
        } catch (statusException: HttpStatusException) {
            handleException(
                statusException,
                HttpResponseStatus.valueOf(statusException.statusCode),
                context.channel(),
                request
            )
        } catch (connectException: ConnectException) {
            handleException(connectException, HttpResponseStatus.BAD_GATEWAY, context.channel(), request)
        } catch (exception: Exception) {
            handleException(exception, HttpResponseStatus.EXPECTATION_FAILED, context.channel(), request)
        } finally {
            httpClient.close()
        }

        return true
    }

    private fun isContentLengthOmitted(response: HttpResponse<InputStream>): Boolean {
        return response.headers().firstValue("content-length").isEmpty
    }

    private fun handleException(
        ex: Exception,
        responseStatus: HttpResponseStatus,
        channel: Channel,
        request: FullHttpRequest
    ) {
        val url = request.headers()[OpenApiUtils.OPENAPI_ORIGINAL_URL] ?: request.uri()

        Notification(
            "com.explyt.spring.notification.web",
            SpringWebBundle.message("explyt.spring.web.notifications"),
            SpringWebBundle.message(
                "explyt.spring.web.error",
                url,
                ex.message ?: ex
            ),
            NotificationType.INFORMATION
        ).notify(null)

        DefaultHttpResponse(HttpVersion.HTTP_1_1, responseStatus)
            .send(channel, request)
    }

    private fun bodyPublisherOf(request: FullHttpRequest): HttpRequest.BodyPublisher {
        if (request.content().readableBytes() <= 0) {
            return BodyPublishers.noBody()
        }

        return BodyPublishers.ofInputStream {
            ByteBufInputStream(request.content())
        }
    }

    private fun mapToHeaders(originalHeaders: Map<String?, List<String>?>): HttpHeaders {
        val httpHeaders = DefaultHttpHeaders()

        for (header in originalHeaders) {
            if (header.key == null) continue

            httpHeaders.set(header.key, header.value)
        }

        return httpHeaders
    }

}
