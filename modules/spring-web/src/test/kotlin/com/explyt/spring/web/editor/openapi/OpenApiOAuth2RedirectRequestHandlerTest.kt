/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.editor.openapi

import com.explyt.spring.test.ExplytBaseLightTestCase
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion

class OpenApiOAuth2RedirectRequestHandlerTest : ExplytBaseLightTestCase() {

    private val handler = OpenApiOAuth2RedirectRequestHandler()
    private val resourcesHandler = OpenApiResourcesRequestHandler()

    fun testServesTheRedirectPath() {
        assertTrue(handler.isSupported(request(REDIRECT_PATH)))
        assertTrue(handler.isSupported(request("$REDIRECT_PATH?code=abc&state=xyz")))
    }

    fun testIgnoresEverythingElse() {
        assertFalse(handler.isSupported(request("/explyt-openapi?key=$SOME_KEY&resource=index.html")))
        assertFalse(handler.isSupported(request("/explyt-openapi_internal-cors")))
        assertFalse(handler.isSupported(request("/oauth2-redirect.html")))
        assertFalse(handler.isSupported(request(REDIRECT_PATH, method = HttpMethod.POST)))
    }

    /**
     * The defect this handler exists for: the authorization server redirects the browser back with
     * its own `Referer`, which the default origin check rejects.
     */
    fun testAcceptsTheRedirectFromAnAuthorizationServer() {
        val redirect = request(REDIRECT_PATH, referer = "https://accounts.google.com/")

        assertTrue(handler.isAccessible(redirect))
    }

    /**
     * Negative control: relaxing the origin check must not have relaxed the `Host` check, which is
     * what stops a host name rebound to the loopback interface.
     */
    fun testRejectsARequestNotAddressedToTheLoopbackInterface() {
        val rebound = request(REDIRECT_PATH, host = "attacker.example:63342")

        assertFalse(handler.isAccessible(rebound))
    }

    /**
     * The resource handler serves the specification opened in the editor, so it must keep rejecting
     * both the redirect path and a foreign referrer.
     */
    fun testResourceHandlerWasNotWidened() {
        assertFalse(resourcesHandler.isSupported(request(REDIRECT_PATH)))

        val fromElsewhere = request(
            "/explyt-openapi?key=$SOME_KEY&resource=specification_file",
            referer = "https://accounts.google.com/"
        )
        assertFalse(resourcesHandler.isAccessible(fromElsewhere))
    }

    fun testRedirectUrlCarriesNoPerSessionKey() {
        val redirectUrl = OpenApiUtils.oauth2RedirectUrl()

        assertTrue(redirectUrl, redirectUrl.endsWith(OpenApiUtils.OPENAPI_OAUTH2_REDIRECT))
        assertFalse(redirectUrl, redirectUrl.contains("key="))
        assertFalse(redirectUrl, redirectUrl.contains("?"))
    }

    fun testRedirectPageIsBundled() {
        val page = javaClass.classLoader
            .getResourceAsStream("htmlTemplates/openapi/oauth2-redirect.html")
            ?.use { it.readBytes().decodeToString() }

        assertNotNull("oauth2-redirect.html is no longer bundled", page)
        assertTrue(
            "the bundled page must be self-contained, a relative script cannot be served",
            page!!.contains("swaggerUIRedirectOauth2") && !page.contains("script src=")
        )
    }

    private fun request(
        uri: String,
        method: HttpMethod = HttpMethod.GET,
        host: String = "localhost:63342",
        referer: String? = null
    ): FullHttpRequest {
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri)
        request.headers().set(HttpHeaderNames.HOST, host)
        referer?.let { request.headers().set(HttpHeaderNames.REFERER, it) }
        return request
    }

    companion object {
        private const val REDIRECT_PATH = OpenApiUtils.OPENAPI_OAUTH2_REDIRECT
        private const val SOME_KEY = "0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0"
    }
}
