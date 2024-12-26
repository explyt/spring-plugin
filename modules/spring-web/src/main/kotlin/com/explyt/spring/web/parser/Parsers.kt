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

package com.explyt.spring.web.parser

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.execution.ParametersListUtil
import io.ktor.http.*
import io.ktor.util.*
import java.util.*

object UrlParser {
    fun parse(urlString: String): HttpMethod {
        val url = Url(urlString)
        val lastPath = url.pathSegments.takeIf { it.isNotEmpty() }?.last()?.takeIf { it.isNotEmpty() } ?: "method"
        val methodName = lastPath.replace("{", "").replace("}", "").replace("-", "").replace("_", "")
        val javaIdentifier = StringUtil.decapitalize(StringUtil.sanitizeJavaIdentifier(methodName))
        return HttpMethod(javaIdentifier, urlString, getHttpParams(url))
    }

    private fun getHttpParams(url: Url): List<HttpParam> {
        return url.pathSegments
            .filter { it.startsWith("{") && it.endsWith("}") }
            .map { HttpParam(HttpParamType.PATH, it.replace("{", "").replace("}", ""), null) } +
                url.parameters.toMap().map { HttpParam(HttpParamType.QUERY, it.key, it.value.joinToString(",")) }
    }
}

object CurlParser {
    fun parse(urlString: String): HttpMethod {
        val replace = urlString.replace("'", "\"")
        val parse = ParametersListUtil.parse(replace)
        if (parse.size < 2) throw RuntimeException("String must be contain minimum two parts 'curl http://url.com/'")
        val curl = parse[0]
        if (curl != "curl") throw RuntimeException("String must be start with 'curl'")
        val parameters = parse.asSequence().drop(1)
            .flatMap { if (it.startsWith("-X") && it != "-X") listOf("-X", it.substring(2)) else listOf(it) }
            .toList()
        var currentUrl: HttpMethod? = null
        var currentMethod = "GET"
        var currentBody: String? = null
        var currentState: ParserState? = null
        val httpParams = mutableListOf<HttpParam>()
        val cookies = mutableListOf<String>()
        var timeOutSet = 0
        var redirect = false
        var outPut = ""
        for (parameter in parameters) {
            if (parameter.startsWith("http://") || parameter.startsWith("https://")) {
                currentUrl = UrlParser.parse(parameter)
            } else if (parameter in ParserState.HEADER.params) {
                currentState = ParserState.HEADER
            } else if (parameter in ParserState.USER_AGENT.params) {
                currentState = ParserState.USER_AGENT
            } else if (parameter in ParserState.USER.params) {
                currentState = ParserState.USER
            } else if (parameter in ParserState.METHOD.params) {
                currentState = ParserState.METHOD
            } else if (parameter in ParserState.COOKIE.params) {
                currentState = ParserState.COOKIE
            } else if (parameter in ParserState.DATA.params) {
                currentState = ParserState.DATA
            } else if (parameter in ParserState.TIMEOUT.params) {
                currentState = ParserState.TIMEOUT
            } else if (parameter in ParserState.OUTPUT.params) {
                currentState = ParserState.OUTPUT
            } else if (parameter in ParserState.REDIRECT.params) {
                redirect = true
            } else if (parameter == "-I" || parameter == "--head") {
                currentMethod = "HEAD"
            } else if (parameter == "--compressed") {
                val httpParam = httpParams.find { it.name == "Accept-Encoding" }
                if (httpParam == null) {
                    httpParams.add(HttpParam(HttpParamType.HEADER, "Accept-Encoding", "deflate, gzip"))
                }
            } else if (currentState == ParserState.HEADER) {
                addHeader(parameter, httpParams)
                currentState = null
            } else if (currentState == ParserState.USER_AGENT) {
                httpParams.add(HttpParam(HttpParamType.HEADER, "User-Agent", parameter))
                currentState = null
            } else if (currentState == ParserState.USER) {
                httpParams.add(HttpParam(HttpParamType.HEADER, "Authorization", getAuthorizationValue(parameter)))
                currentState = null
            } else if (currentState == ParserState.COOKIE) {
                cookies.add(parameter.trim())
                currentState = null
            } else if (currentState == ParserState.METHOD) {
                currentMethod = parameter
                currentState = null
            } else if (currentState == ParserState.DATA) {
                currentBody = if (currentBody.isNullOrEmpty()) parameter else "$currentBody&$parameter"
                currentState = null
                setDataContentType(httpParams, currentBody)
            } else if (currentState == ParserState.TIMEOUT) {
                try {
                    timeOutSet = parameter.toInt()
                } catch (_: Exception) {
                }
                currentState = null
            } else if (currentState == ParserState.OUTPUT) {
                outPut = parameter.trim()
                currentState = null
            }
        }
        if (currentUrl == null) throw RuntimeException("Url is empty!")
        if (cookies.isNotEmpty()) {
            httpParams.add(HttpParam(HttpParamType.HEADER, "Cookie", cookies.joinToString("; ")))
        }
        if (currentBody != null) {
            httpParams.add(HttpParam(HttpParamType.DATA, "requestBody", currentBody))
        }
        return currentUrl.copy(
            type = currentMethod, params = currentUrl.params + httpParams,
            timeOutSec = timeOutSet, redirect = redirect, outPut = outPut
        )
    }

    private fun setDataContentType(httpParams: MutableList<HttpParam>, currentBody: String?) {
        val httpParam = httpParams.find { it.name == "Content-Type" }
        if (httpParam == null) {
            if (currentBody?.startsWith("{") == true) {
                httpParams.add(HttpParam(HttpParamType.HEADER, "Content-Type", "application/json"))
            } else {
                httpParams.add(HttpParam(HttpParamType.HEADER, "Content-Type", "application/x-www-form-urlencoded"))
            }
        }
    }

    private fun getAuthorizationValue(parameter: String): String {
        return "Basic " + Base64.getEncoder().encodeToString(parameter.encodeToByteArray())
    }

    private fun addHeader(parameter: String, httpParams: MutableList<HttpParam>) {
        try {
            val split = parameter.split(":")
            httpParams.add(HttpParam(HttpParamType.HEADER, split[0].trim(), split[1].trim()))
        } catch (_: Exception) {
        }
    }

    private enum class ParserState(val params: Set<String>) {
        USER_AGENT(setOf("-A", "--user-agent")),
        HEADER(setOf("-H", "--header")),
        DATA(setOf("-d", "--data", "--data-ascii")),
        USER(setOf("-u", "--user")),
        METHOD(setOf("-X", "--request")),
        COOKIE(setOf("-b", "--cookie")),
        TIMEOUT(setOf("-m", "--max-time")),
        REDIRECT(setOf("-L", "--location", "--location-trusted")),
        OUTPUT(setOf("-o", "--output")),
    }
}

data class HttpMethod(
    val name: String, val url: String, val params: List<HttpParam>, val type: String = "GET",
    val redirect: Boolean = false, val timeOutSec: Int = 0, val outPut: String = "",
) {
    fun getMappingHttpValue(): String {
        return if (url.contains("?")) url.substringBefore("?") else url
    }
}

data class HttpParam(val type: HttpParamType, val name: String, val value: String?) {
    fun toJavaIdentifier() = StringUtil.decapitalize(StringUtil.sanitizeJavaIdentifier(name))
}

enum class HttpParamType { QUERY, PATH, HEADER, DATA }