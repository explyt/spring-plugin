/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.HttpRequestHandler

/**
 * Serves the Swagger UI OAuth2 redirect page of the OpenAPI preview at the stable
 * [OpenApiUtils.OPENAPI_OAUTH2_REDIRECT] path.
 *
 * It is deliberately separate from [OpenApiResourcesRequestHandler]: the redirect arrives as a
 * top-level navigation from the authorization server, so it carries that server's `Referer`, and
 * the default [isAccessible] rejects a non-local referrer. Relaxing the check on the resource
 * handler would widen it to `specification_file` as well, which serves the specification opened in
 * the editor. This handler only ever returns one static page that holds no project data.
 */
class OpenApiOAuth2RedirectRequestHandler : HttpRequestHandler() {

    override fun isSupported(request: FullHttpRequest): Boolean =
        request.method() == HttpMethod.GET && request.uri().startsWith(OpenApiUtils.OPENAPI_OAUTH2_REDIRECT)

    /**
     * Accepts the authorization server as the origin of the redirect.
     *
     * Only the origin half of [isAccessible] is relaxed; its `Host` header check still restricts
     * the handler to the loopback interface, so a host rebound to `127.0.0.1` remains rejected.
     */
    override fun isOriginAllowed(request: HttpRequest): OriginCheckResult = OriginCheckResult.ALLOW

    override fun process(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): Boolean {
        val content = javaClass.classLoader
            .getResourceAsStream(OAUTH2_REDIRECT_RESOURCE_PATH)
            ?.use { it.readBytes() }
            ?: return false

        // The content type follows the extension of this name, not the extension-less request URI.
        return sendData(content, OAUTH2_REDIRECT_FILE_NAME, request, context.channel(), EmptyHttpHeaders.INSTANCE)
    }

    companion object {
        private const val OAUTH2_REDIRECT_FILE_NAME = "oauth2-redirect.html"
        private const val OAUTH2_REDIRECT_RESOURCE_PATH = "htmlTemplates/openapi/$OAUTH2_REDIRECT_FILE_NAME"
    }
}
