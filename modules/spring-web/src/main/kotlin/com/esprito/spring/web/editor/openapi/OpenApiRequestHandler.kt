package com.esprito.spring.web.editor.openapi

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.EmptyHttpHeaders
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.HttpRequestHandler
import java.util.*

class OpenApiRequestHandler : HttpRequestHandler() {

    override fun process(
        urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext
    ): Boolean {
        val channel = context.channel()

        val key = urlDecoder.parameters()["key"]?.firstOrNull()?.let { UUID.fromString(it) } ?: return false
        val resource = urlDecoder.parameters()["resource"]?.firstOrNull() ?: return false

        val resourceFile = getResource(key, resource) ?: return false
        resourceFile.charset = Charsets.UTF_8
        val content = resourceFile.contentsToByteArray().decodeToString()

        val toSend =
            if (resource == "index.html") {
                val resourceUrl = OpenApiUtils.resourceUrl(key)
                content.replace("{RESOURCE_URL}", resourceUrl)
            } else {
                content
            }

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

        val pluginId = PluginId.findId("com.esprito.spring") ?: return null
        val mainTemplatePath = "htmlTemplates/openapi/$resource"
        val mainTemplateUrl = PluginManagerCore.getPlugin(pluginId)
            ?.pluginClassLoader
            ?.getResource(mainTemplatePath) ?: return null
        return VfsUtil.findFileByURL(mainTemplateUrl)
    }

    override fun isSupported(request: FullHttpRequest): Boolean {
        if (request.method() != HttpMethod.GET) return false

        return request.uri().startsWith("/__explyt-openapi?")
    }

}