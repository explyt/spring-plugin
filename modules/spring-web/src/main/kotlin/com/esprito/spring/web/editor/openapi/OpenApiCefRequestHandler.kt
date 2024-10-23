package com.esprito.spring.web.editor.openapi

import com.intellij.openapi.util.registry.Registry
import org.cef.network.CefRequest

class OpenApiCefRequestHandler {

    fun mutateRequest(request: CefRequest) {
        request.setHeaderByName(OpenApiUtils.OPENAPI_ORIGINAL_URL, request.url, true)
        request.url = OpenApiUtils.proxyUrl()
    }

    fun isApplicable(request: CefRequest): Boolean =
        Registry.`is`("openapi.ui.proxy.enable")
                && !request.url.contains("__explyt-openapi")

}