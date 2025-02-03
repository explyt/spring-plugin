package com.explyt.spring.web.httpclient.action

import org.junit.Assert.assertEquals
import org.junit.Test

class HttpRunFileActionTest {
    private val requests = """
      GET https://api.openweathermap.org/data/2.5/weather?lat=1&lon=1

      ###
      POST https://petstore.swagger.io/v2/pet
      Content-Type: application/json
      Authorization: Bearer token

      {
        "name": "John Doe",
        "email": "john.doe@example.com"
      }
      
      ###

      GET https://api.openweathermap.org/data/2.5/weather?lat=2&lon=2

      ###

      GET https://api.openweathermap.org/data/2.5/weather?lat=3&lon=3
  """.trimIndent()

    @Test
    fun testFirstRequest() {
        val lines = requests.split(System.lineSeparator()).toList()
        val result = HttpRunFileAction.getSingleRequestByLineNumber(lines, 0)
        assertEquals("GET https://api.openweathermap.org/data/2.5/weather?lat=1&lon=1", result.trimIndent())
    }

    @Test
    fun testSecondRequest() {
        val lines = requests.split(System.lineSeparator()).toList()
        val result = HttpRunFileAction.getSingleRequestByLineNumber(lines, 4)
        assertEquals(
            """
      POST https://petstore.swagger.io/v2/pet
      Content-Type: application/json
      Authorization: Bearer token

      {
        "name": "John Doe",
        "email": "john.doe@example.com"
      }
        """.trimIndent(), result.trimIndent()
        )
    }

    @Test
    fun testThirdRequest() {
        val lines = requests.split(System.lineSeparator()).toList()
        val result = HttpRunFileAction.getSingleRequestByLineNumber(lines, 16)
        assertEquals("GET https://api.openweathermap.org/data/2.5/weather?lat=2&lon=2", result.trimIndent())
    }

    @Test
    fun testLastRequest() {
        val lines = requests.split(System.lineSeparator()).toList()
        val result = HttpRunFileAction.getSingleRequestByLineNumber(lines, 18)
        assertEquals("GET https://api.openweathermap.org/data/2.5/weather?lat=3&lon=3", result.trimIndent())
    }
}