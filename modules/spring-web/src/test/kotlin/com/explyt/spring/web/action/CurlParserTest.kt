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


import com.explyt.spring.web.parser.CurlParser
import com.explyt.spring.web.parser.HttpParam
import com.explyt.spring.web.parser.HttpParamType.DATA
import com.explyt.spring.web.parser.HttpParamType.HEADER
import org.junit.Assert
import org.junit.Test

class CurlParserTest {

    @Test
    fun curlData() {
        val curlString =
            "curl -X POST -H \"Content-Type: application/json\" -d \"{\\\"title\\\": \\\"Заголовок \\\", \\\"body\\\": \\\"Тело\\\", \\\"userId\\\": 1}\"" +
                    " \"https://jsonplaceholder.typicode.com/posts\""
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(
            HttpParam(HEADER, "Content-Type", "application/json"),
            HttpParam(DATA, "requestBody", "{\"title\": \"Заголовок \", \"body\": \"Тело\", \"userId\": 1}")
        )

        Assert.assertEquals("POST", httpMethod.type)
        Assert.assertEquals("https://jsonplaceholder.typicode.com/posts", httpMethod.url)
        Assert.assertEquals("posts", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlManyHeaders() {
        val curlString = "curl 'http://google.com/' \n" +
                "  -H 'Accept-Encoding: gzip, deflate, sdch' \n" +
                "  -H 'Accept-Language: en-US,en;q=0.8,da;q=0.6' \n" +
                "  -H 'Upgrade-Insecure-Requests: 1' \n" +
                "  -H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36' \n" +
                "  -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8' \n" +
                "  -H 'Connection: keep-alive' \n" +
                "  --cookie \"USER_TOKEN=Yes\" \n" +
                "  --compressed"
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(
            HttpParam(HEADER, "Accept-Encoding", "gzip, deflate, sdch"),
            HttpParam(HEADER, "Accept-Language", "en-US,en;q=0.8,da;q=0.6"),
            HttpParam(HEADER, "Upgrade-Insecure-Requests", "1"),
            HttpParam(
                HEADER,
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36"
            ),
            HttpParam(HEADER, "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"),
            HttpParam(HEADER, "Connection", "keep-alive"),
            HttpParam(HEADER, "Cookie", "USER_TOKEN=Yes")
        )

        Assert.assertEquals("GET", httpMethod.type)
        Assert.assertEquals("http://google.com/", httpMethod.url)
        Assert.assertEquals("method", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlCompressed() {
        val curlString = "curl -XPOST -H \"Content-Type: application/json\"   --compressed " +
                " \"https://jsonplaceholder.typicode.com/posts\""
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(
            HttpParam(HEADER, "Content-Type", "application/json"),
            HttpParam(HEADER, "Accept-Encoding", "deflate, gzip")
        )

        Assert.assertEquals("POST", httpMethod.type)
        Assert.assertEquals("https://jsonplaceholder.typicode.com/posts", httpMethod.url)
        Assert.assertEquals("posts", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlJsonBodyWithEmptyEncoding() {
        val curlString =
            "curl -XPUT -d \"{\\\"title\\\": \\\"Заголовок \\\", \\\"body\\\": \\\"Тело\\\", \\\"userId\\\": 1}\"" +
                    " \"https://jsonplaceholder.typicode.com/posts\""
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(
            HttpParam(HEADER, "Content-Type", "application/json"),
            HttpParam(DATA, "requestBody", "{\"title\": \"Заголовок \", \"body\": \"Тело\", \"userId\": 1}")
        )

        Assert.assertEquals("PUT", httpMethod.type)
        Assert.assertEquals("https://jsonplaceholder.typicode.com/posts", httpMethod.url)
        Assert.assertEquals("posts", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlBodyWithEmptyEncoding() {
        val curlString =
            "curl -XPUT -d \"key=value\" \"https://jsonplaceholder.typicode.com/posts\""
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(
            HttpParam(HEADER, "Content-Type", "application/x-www-form-urlencoded"),
            HttpParam(DATA, "requestBody", "key=value")
        )

        Assert.assertEquals("PUT", httpMethod.type)
        Assert.assertEquals("https://jsonplaceholder.typicode.com/posts", httpMethod.url)
        Assert.assertEquals("posts", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlAuthorizationBasic() {
        val curlString = "curl --user username:password http://url/api/test"
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(HttpParam(HEADER, "Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ="))

        Assert.assertEquals("GET", httpMethod.type)
        Assert.assertEquals("http://url/api/test", httpMethod.url)
        Assert.assertEquals("test", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
    }

    @Test
    fun curlHeaders() {
        val curlString = "curl -I https://www.keycdn.com"
        val httpMethod = CurlParser.parse(curlString)

        Assert.assertEquals("HEAD", httpMethod.type)
        Assert.assertEquals("https://www.keycdn.com", httpMethod.url)
        Assert.assertEquals("method", httpMethod.name)
        Assert.assertTrue(httpMethod.params.isEmpty())
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlSimpleCookie() {
        val curlString = "curl -XGET https://www.keycdn.com -b JSESSIONID=415"
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(HttpParam(HEADER, "Cookie", "JSESSIONID=415"))

        Assert.assertEquals("GET", httpMethod.type)
        Assert.assertEquals("https://www.keycdn.com", httpMethod.url)
        Assert.assertEquals("method", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
    }

    @Test
    fun curlManyCookie() {
        val curlString = "curl -XGET https://www.keycdn.com -b JSESSIONID=415 -b token=yes --cookie key=value"
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(HttpParam(HEADER, "Cookie", "JSESSIONID=415; token=yes; key=value"))

        Assert.assertEquals("GET", httpMethod.type)
        Assert.assertEquals("https://www.keycdn.com", httpMethod.url)
        Assert.assertEquals("method", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertFalse(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlRedirect() {
        val curlString = "curl -XGET -L -H \"key: value\" https://www.keycdn.com -b JSESSIONID=415"
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(HttpParam(HEADER, "key", "value"), HttpParam(HEADER, "Cookie", "JSESSIONID=415"))

        Assert.assertEquals("GET", httpMethod.type)
        Assert.assertEquals("https://www.keycdn.com", httpMethod.url)
        Assert.assertEquals("method", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertTrue(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
    }

    @Test
    fun curlTimeout() {
        val curlString = "curl -XGET -m 5 -H \"key: value\" https://www.keycdn.com --location"
        val httpMethod = CurlParser.parse(curlString)
        val params = listOf(HttpParam(HEADER, "key", "value"))

        Assert.assertEquals("GET", httpMethod.type)
        Assert.assertEquals("https://www.keycdn.com", httpMethod.url)
        Assert.assertEquals("method", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertTrue(httpMethod.redirect)
        Assert.assertEquals(5, httpMethod.timeOutSec)
        Assert.assertEquals("", httpMethod.outPut)
    }

    @Test
    fun curlOutput() {
        val curlString = "curl -XGET https://www.keycdn.com -L -o /home/file.txt"
        val httpMethod = CurlParser.parse(curlString)
        val params = emptyList<HttpParam>()

        Assert.assertEquals("GET", httpMethod.type)
        Assert.assertEquals("https://www.keycdn.com", httpMethod.url)
        Assert.assertEquals("method", httpMethod.name)
        Assert.assertEquals(params, httpMethod.params)
        Assert.assertTrue(httpMethod.redirect)
        Assert.assertEquals(0, httpMethod.timeOutSec)
        Assert.assertEquals("/home/file.txt", httpMethod.outPut)
    }
}