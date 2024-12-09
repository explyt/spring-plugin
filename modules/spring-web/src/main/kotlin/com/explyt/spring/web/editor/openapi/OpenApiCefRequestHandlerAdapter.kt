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

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest

class OpenApiCefRequestHandlerAdapter : CefRequestHandlerAdapter() {
    val requestHandler: OpenApiCefRequestHandler by lazy { OpenApiCefRequestHandler() }

    override fun getResourceRequestHandler(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest?,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String?,
        disableDefaultHandling: BoolRef?
    ): CefResourceRequestHandler {
        return object : CefResourceRequestHandlerAdapter() {
            override fun onBeforeResourceLoad(browser: CefBrowser?, frame: CefFrame?, request: CefRequest?): Boolean {
                if (request == null) return false

                var currentUrl = request.url

                currentUrl = currentUrl.substringBefore("?anchor=").let { baseUrl ->
                    val anchor = currentUrl.substringAfter("?anchor=", "")
                    baseUrl + if (anchor.isNotEmpty()) "#$anchor" else ""
                }

                request.url = currentUrl

                if (requestHandler.isApplicable(request)) {
                    requestHandler.mutateRequest(request)
                }

                return super.onBeforeResourceLoad(browser, frame, request)
            }
        }
    }

}