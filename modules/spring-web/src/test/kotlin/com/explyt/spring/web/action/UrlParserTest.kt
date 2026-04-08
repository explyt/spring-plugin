/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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