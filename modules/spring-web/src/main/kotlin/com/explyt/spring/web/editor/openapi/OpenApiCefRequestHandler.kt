/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import org.cef.network.CefRequest
import java.util.*

class OpenApiCefRequestHandler {

    fun mutateRequest(request: CefRequest) {
        request.setHeaderByName(OpenApiUtils.OPENAPI_ORIGINAL_URL, request.url, true)
        val authHeader = request.getHeaderByName("Authorization")
        if (authHeader.isNotBlank()) {
            request.setHeaderByName(OpenApiUtils.OPENAPI_AUTH_HEADER, authHeader, true)
        }
        val proxyUrl = OpenApiUtils.proxyUrl()
        request.url = proxyUrl +
                if (proxyUrl.contains('?')) "&" else "?" +
                        "explytCacheBusting=${UUID.randomUUID()}"

    }

    fun isApplicable(request: CefRequest): Boolean {
        return !request.url.contains(OpenApiUtils.EXPLYT_OPENAPI)
    }

}