/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

private const val CO_ROUTER_PATH = "/x"
private const val SERVLET_ROUTER_PATH = "/servlet/x"

class SpringBootApplicationMcpToolsetAnyMethodRouteTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.kotlin_1_9_22,
        TestLibrary.kotlin_coroutines_1_7_1,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        addCoRouterRoute()
        addServletRouterRoute()
    }

    fun testAnyMethodPathRoutesAreListedForAllMethods() = runBlockingReleasingWriteIntent<Unit> {
        assertEquals(listOf("ALL"), listedMethodsOf(CO_ROUTER_PATH))
        assertEquals(listOf("ALL"), listedMethodsOf(SERVLET_ROUTER_PATH))
    }

    fun testAnyMethodPathRouteMatchesEveryHttpMethodFilter() = runBlockingReleasingWriteIntent<Unit> {
        val response = mapper.readTree(
            toolset.findEndpoint(
                urlPattern = CO_ROUTER_PATH,
                httpMethod = "POST",
                projectPath = projectPath(),
            )
        )

        assertTrue(
            response["endpoints"].any {
                it["fullPath"].asText() == CO_ROUTER_PATH &&
                        it["httpMethods"].map { method -> method.asText() } == listOf("ALL")
            }
        )
    }

    fun testCoRouterAnyMethodPathRouteContractNamesItsHandler() = runBlockingReleasingWriteIntent<Unit> {
        assertContractNamesHandler(
            path = CO_ROUTER_PATH,
            registrationClass = "explyt.anymethod.CoRoutes",
            factory = "coRoutes",
            handler = "explyt.anymethod.CoHandler.handle"
        )
    }

    fun testServletRouterAnyMethodPathRouteContractNamesItsHandler() = runBlockingReleasingWriteIntent<Unit> {
        assertContractNamesHandler(
            path = SERVLET_ROUTER_PATH,
            registrationClass = "explyt.anymethod.MvcRoutes",
            factory = "mvcRoutes",
            handler = "explyt.anymethod.MvcHandler.handle"
        )
    }

    private suspend fun assertContractNamesHandler(
        path: String,
        registrationClass: String,
        factory: String,
        handler: String
    ) {
        assertEquals(listOf("ALL"), listedMethodsOf(path))
        val contract = contractOf(path)
        val json = contract.toString()
        assertEquals(json, listOf("ALL"), contract["httpMethods"].map { it.asText() })
        assertEquals(json, registrationClass, contract["controllerClass"].asText())
        assertEquals(json, factory, contract["methodName"].asText())
        assertEquals(json, "PARTIAL", contract["contractStatus"].asText())
        assertTrue(json, contract["contractUnavailableReason"].asText().contains("ServerRequest"))
        assertEquals(json, listOf(handler), contract["serviceCalls"].map { it["target"].asText() })
        assertEquals(json, handler, contract["serviceCall"]["target"].asText())
        assertFalse(json, json.contains("\"path\"", ignoreCase = true))
    }

    private suspend fun listedMethodsOf(path: String): List<String> =
        mapper.readTree(toolset.getHttpEndpoints(projectPath = projectPath(), compact = true))["endpoints"]
            .single { it["fullPath"].asText() == path }["httpMethods"]
            .map { it.asText() }

    private suspend fun contractOf(path: String): JsonNode =
        mapper.readTree(toolset.getEndpointContract(urlPattern = path, projectPath = projectPath()))["endpoints"]
            .single { it["fullPath"].asText() == path }

    private fun projectPath(): String = project.basePath ?: ""

    private fun addCoRouterRoute() {
        myFixture.addFileToProject(
            "explyt/anymethod/CoRoutes.kt",
            """
            package explyt.anymethod

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerRequest
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.buildAndAwait
            import org.springframework.web.reactive.function.server.coRouter

            @Component
            class CoHandler {
                suspend fun handle(request: ServerRequest): ServerResponse = ServerResponse.ok().buildAndAwait()
            }

            @Configuration
            class CoRoutes {
                @Bean
                fun coRoutes(handler: CoHandler): RouterFunction<ServerResponse> = coRouter {
                    path("$CO_ROUTER_PATH", handler::handle)
                }
            }
            """.trimIndent()
        )
    }

    private fun addServletRouterRoute() {
        myFixture.addFileToProject(
            "explyt/anymethod/MvcRoutes.kt",
            """
            package explyt.anymethod

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
            import org.springframework.web.servlet.function.RouterFunction
            import org.springframework.web.servlet.function.ServerRequest
            import org.springframework.web.servlet.function.ServerResponse
            import org.springframework.web.servlet.function.router

            @Component
            class MvcHandler {
                fun handle(request: ServerRequest): ServerResponse = ServerResponse.ok().build()
            }

            @Configuration
            class MvcRoutes {
                @Bean
                fun mvcRoutes(handler: MvcHandler): RouterFunction<ServerResponse> = router {
                    path("$SERVLET_ROUTER_PATH", handler::handle)
                }
            }
            """.trimIndent()
        )
    }
}
