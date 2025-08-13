/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.web.httpclient

import com.explyt.spring.web.language.http.psi.*
import com.explyt.spring.web.providers.EndpointRunLineMarkerProvider
import com.explyt.spring.web.util.OpenApiFileUtil.Companion.DEFAULT_SERVER
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiErrorElement
import com.intellij.util.ProcessingContext
import java.nio.charset.Charset

class HttpClientCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), HttpClientCompletionProvider())
    }
}

private class HttpClientCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val psiElement = parameters.position.parent
        val completions = when (psiElement) {
            is HttpRequestTarget -> {
                val psiText = psiElement.parent?.text?.trim() ?: ""
                if (psiText.startsWith("Intellij"))
                    listOf("https://explyt.ai/") + getHttpMethods()
                else if (psiText.contains(" "))
                    getHttpVariants(psiElement)
                else
                    getHttpMethods()
            }

            is HttpMethod -> {
                getHttpMethods()
            }

            is HttpFieldName -> {
                getHttpHeaders()
            }

            is PsiErrorElement -> {
                getHttpHeaders()
            }

            is HttpRequestBody -> {
                listOf("< my-file.json", "{\"id\":1,\"name\":\"name\"}")
            }

            is HttpFieldValue -> {
                getHeaderValues(psiElement)
            }

            else -> emptyList()
        }

        val lookupElementBuilders = completions.map { LookupElementBuilder.create(it) }
        result.addAllElements(lookupElementBuilders)
    }

    private fun getHttpVariants(psiElement: HttpRequestTarget): Collection<String> {
        val servers = EndpointRunLineMarkerProvider.applyServerPortSettings(psiElement)
        return (listOf("https://", "https://explyt.ai/") + servers + DEFAULT_SERVER).toSet()
    }

    private fun getHttpMethods(): List<String> {
        return SpringWebUtil.REQUEST_METHODS.map { it.uppercase() }
    }

    private fun getHttpHeaders(): List<String> {
        return listOf(
            "Cache-Control", "Date",
            "Transfer-Encoding",
            "Accept",
            "Accept-Charset",
            "Accept-Encoding",
            "Accept-Language",
            "Authorization",
            "Expect",
            "From",
            "Host",
            "Proxy-Authorization",
            "Range",
            "Referer",
            "User-Agent",
            "Content-Encoding",
            "Content-Language",
            "Content-Length",
            "Content-Location",
            "Content-MD5",
            "Content-Range",
            "Content-Type",
            "Expires",
            "Last-Modified",
            "Content-Disposition",
        ).map { "$it: " }
    }

    private fun getHeaderValues(psiElement: HttpFieldValue): List<String> {
        val headerName = (psiElement.parent as? HttpFieldLine)?.fieldName?.text ?: return emptyList()
        return if (headerName.contains("-Encoding")) {
            getTransferEncodings()
        } else if (headerName == "Accept" || headerName == "Content-Type") {
            getMimeTypes()
        } else if (headerName.contains("-Charset")) {
            getCharsets()
        } else {
            emptyList()
        }
    }

    private fun getTransferEncodings(): List<String> {
        return listOf("chunked", "compress", "deflate", "gzip", "identity")
    }

    private fun getMimeTypes(): List<String> {
        return listOf(
            "*/*", "image/*", "message/http",
            "text/html", "text/plain", "text/css", "text/javascript", "text/xml", "text/csv",
            "application/json", "application/xml", "application/pdf", "application/octet-stream",
            "application/x-www-form-urlencoded",
            "image/jpeg", "image/png", "image/gif", "image/svg+xml",
            "audio/mpeg", "audio/wav", "audio/ogg",
            "video/mp4", "video/webm", "video/ogg",
            "multipart/form-data", "multipart/mixed", "multipart/alternative",
        )
    }

    private fun getCharsets(): List<String> {
        return Charset.availableCharsets().keys.toList()
    }
}
