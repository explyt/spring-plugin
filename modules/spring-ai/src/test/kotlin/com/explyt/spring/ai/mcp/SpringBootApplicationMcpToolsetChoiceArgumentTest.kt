/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpExpectedError
import kotlinx.coroutines.runBlocking

/**
 * An argument naming one of a fixed set of values answers an unknown value with an error listing the valid ones.
 *
 * Filtering by an unknown value answers with nothing, which a caller cannot tell from "nothing of that kind exists": an
 * agent whose tool schema still advertised `SPRING_BOOT` read the empty answer as "this project has no Actuator".
 */
class SpringBootApplicationMcpToolsetChoiceArgumentTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("springBootApp", "")
    }

    private fun projectPath(): String = project.basePath ?: ""

    fun testUnknownEndpointTypeIsRejectedWithTheValidValues() = runBlocking<Unit> {
        val error = rejected { toolset.getHttpEndpoints(projectPath = projectPath(), endpointType = "BOGUS") }

        assertEquals(
            "Unknown endpointType 'BOGUS'. Valid values: " +
                    "SPRING_MVC, SPRING_HTTP_EXCHANGE, SPRING_JAX_RS, SPRING_WEBFLUX, SPRING_OPEN_FEIGN, ACTUATOR.",
            error
        )
    }

    /** `SPRING_BOOT` was once advertised but never matched an endpoint the tool lists, so it gets no alias. */
    fun testFormerlyAdvertisedSpringBootIsRejectedToo() = runBlocking<Unit> {
        val error = rejected { toolset.getHttpEndpoints(projectPath = projectPath(), endpointType = "SPRING_BOOT") }

        assertTrue(error, error.startsWith("Unknown endpointType 'SPRING_BOOT'. Valid values: "))
        assertTrue(error, "ACTUATOR" in error)
    }

    fun testEndpointTypeIsCaseInsensitiveAndEmptyMeansAll() = runBlocking<Unit> {
        val all = mapper.readTree(toolset.getHttpEndpoints(projectPath = projectPath()))
        val mvc = mapper.readTree(toolset.getHttpEndpoints(projectPath = projectPath(), endpointType = " spring_mvc "))

        assertTrue("The fixture has Spring MVC endpoints", mvc["totalCount"].asInt() > 0)
        assertEquals(all["totalCount"].asInt(), mvc["totalCount"].asInt())
    }

    fun testUnknownHttpMethodIsRejectedWithTheValidValues() = runBlocking<Unit> {
        val error = rejected {
            toolset.findEndpoint(urlPattern = "/api/demo/items", projectPath = projectPath(), httpMethod = "FETCH")
        }

        assertEquals("Unknown httpMethod 'FETCH'. Valid values: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS.", error)
    }

    fun testHttpMethodIsCaseInsensitive() = runBlocking<Unit> {
        val found = mapper.readTree(
            toolset.findEndpoint(urlPattern = "/api/demo/items/{id}", projectPath = projectPath(), httpMethod = "get")
        )

        assertEquals("/api/demo/items/{id}", found["endpoints"][0]["fullPath"].asText())
    }

    fun testUnknownContractHttpMethodIsRejected() = runBlocking<Unit> {
        val error = rejected {
            toolset.getEndpointContract(urlPattern = "/api/demo/items", projectPath = projectPath(), httpMethod = "FETCH")
        }

        assertTrue(error, error.startsWith("Unknown httpMethod 'FETCH'. Valid values: GET,"))
    }

    fun testUnknownBeanTypeIsRejectedWithTheValidValues() = runBlocking<Unit> {
        val error = rejected {
            toolset.applicationBeans(
                applicationClassName = "com.example.app.DemoApplication",
                projectPath = projectPath(),
                beanType = "SERVICE",
            )
        }

        assertEquals(
            "Unknown beanType 'SERVICE'. Valid values: ASPECT, MESSAGE_MAPPING, CONTROLLER, AUTO_CONFIGURATION, " +
                    "CONFIGURATION_PROPERTIES, CONFIGURATION, REPOSITORY, COMPONENT.",
            error
        )
    }

    fun testUnknownBeanSourceIsRejectedWithTheValidValues() = runBlocking<Unit> {
        val error = rejected {
            toolset.applicationBeans(
                applicationClassName = "com.example.app.DemoApplication",
                projectPath = projectPath(),
                beanType = "COMPONENT",
                source = "LATEST",
            )
        }

        assertEquals("Unknown source 'LATEST'. Valid values: AUTO, STATIC, NATIVE.", error)
    }

    fun testBeanTypeAndSourceAreCaseInsensitive() = runBlocking<Unit> {
        val page = mapper.readTree(
            toolset.applicationBeans(
                applicationClassName = "com.example.app.DemoApplication",
                projectPath = projectPath(),
                beanType = "component",
                source = "static",
            )
        )

        assertEquals("Expected an OK envelope, got $page", "OK", page["status"]?.asText())
        assertTrue("The fixture has components", page["beans"].size() > 0)
    }

    private suspend fun rejected(call: suspend () -> String): String {
        try {
            val answer = call()
            fail("Expected the argument to be rejected, got $answer")
            error("unreachable")
        } catch (error: McpExpectedError) {
            return error.message.orEmpty()
        }
    }
}
