package com.esprito.spring.web.editor.openapi

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

                if (requestHandler.isApplicable(request)) {
                    requestHandler.mutateRequest(request)
                }

                return super.onBeforeResourceLoad(browser, frame, request)
            }
        }
    }

}