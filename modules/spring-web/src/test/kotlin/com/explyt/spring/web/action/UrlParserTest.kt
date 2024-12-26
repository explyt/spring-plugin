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

package com.explyt.spring.web.action


import com.explyt.spring.web.parser.HttpParam
import com.explyt.spring.web.parser.HttpParamType.PATH
import com.explyt.spring.web.parser.HttpParamType.QUERY
import com.explyt.spring.web.parser.UrlParser
import org.junit.Assert
import org.junit.Test

class UrlParserTest {

    @Test
    fun absolutUrl() {
        val urlString = "https://test-host.com/api/users/{login}/tmp/hubs?page=1&perPage=10"
        val httMethod = UrlParser.parse(urlString)
        Assert.assertEquals("hubs", httMethod.name)
        Assert.assertEquals("https://test-host.com/api/users/{login}/tmp/hubs", httMethod.getMappingHttpValue())
        val params =
            listOf(HttpParam(PATH, "login", null), HttpParam(QUERY, "page", "1"), HttpParam(QUERY, "perPage", "10"))
        Assert.assertEquals(params, httMethod.params)
    }

    @Test
    fun relativeUrl() {
        val urlString = "api/users/{login}/tmp/{id}?page=1&perPage=10"
        val httMethod = UrlParser.parse(urlString)
        Assert.assertEquals("id", httMethod.name)
        Assert.assertEquals("api/users/{login}/tmp/{id}", httMethod.getMappingHttpValue())
        val params = listOf(
            HttpParam(PATH, "login", null), HttpParam(PATH, "id", null),
            HttpParam(QUERY, "page", "1"), HttpParam(QUERY, "perPage", "10")
        )
        Assert.assertEquals(params, httMethod.params)
    }

    @Test
    fun relativeUrlWithSlash() {
        val urlString = "/api/users/{login}/tmp/{id}?page=1&perPage=10"
        val httMethod = UrlParser.parse(urlString)
        Assert.assertEquals("id", httMethod.name)
        Assert.assertEquals("/api/users/{login}/tmp/{id}", httMethod.getMappingHttpValue())
        val params = listOf(
            HttpParam(PATH, "login", null), HttpParam(PATH, "id", null),
            HttpParam(QUERY, "page", "1"), HttpParam(QUERY, "perPage", "10")
        )
        Assert.assertEquals(params, httMethod.params)
    }
}