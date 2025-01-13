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

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.HttpRequestHandler
import java.util.*

class OpenApiResourcesRequestHandler : HttpRequestHandler() {

    override fun process(
        urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext
    ): Boolean {
        val channel = context.channel()

        val key = urlDecoder.parameters()["key"]?.firstOrNull()?.let { UUID.fromString(it) } ?: return false
        val resource = urlDecoder.parameters()["resource"]?.firstOrNull() ?: return false

        val resourceFile = getResource(key, resource) ?: return false
        resourceFile.charset = Charsets.UTF_8
        val content = resourceFile.contentsToByteArray().decodeToString()

        val themeFolder = if (JBColor.isBright()) "light" else "dark"

        val toSend =
            if (resource == "index.html") {
                val resourceUrl = OpenApiUtils.resourceUrl(key)
                content
                    .replace("{RESOURCE_URL}", resourceUrl)
            } else {
                content
            }
                .replace("{THEME_FOLDER}", themeFolder)

        sendData(
            toSend.toByteArray(Charsets.UTF_8),
            resourceFile.name,
            request,
            channel,
            EmptyHttpHeaders.INSTANCE
        )

        return true
    }

    private fun getResource(key: UUID, resource: String): VirtualFile? {
        if (resource == "specification_file")
            return OpenApiUtils.getFile(key)

        val pluginId = PluginId.findId("com.explyt.spring") ?: return null
        val mainTemplatePath = "htmlTemplates/openapi/$resource"
        val mainTemplateUrl = PluginManagerCore.getPlugin(pluginId)
            ?.pluginClassLoader
            ?.getResource(mainTemplatePath) ?: return null
        return VfsUtil.findFileByURL(mainTemplateUrl)
    }

    override fun isSupported(request: FullHttpRequest): Boolean {
        if (request.method() != HttpMethod.GET) return false

        return request.uri().startsWith("/${OpenApiUtils.EXPLYT_OPENAPI}?")
    }

}