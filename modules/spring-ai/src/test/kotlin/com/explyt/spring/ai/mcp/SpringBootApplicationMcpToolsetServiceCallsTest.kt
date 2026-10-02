/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * Which calls on injected beans `explyt_get_spring_endpoint_contract` lists for a handler.
 *
 * A handler commonly resolves a tenant or checks access through one bean before it calls the service that answers
 * the request, so the first such call alone pointed callers at the guard. Every call is listed in source order.
 */
class SpringBootApplicationMcpToolsetServiceCallsTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject("com/example/app/web/OrdersController.kt", SOURCE)
    }

    /** The guard is called first, the service second: both are listed, and `serviceCall` stays the first. */
    fun testGuardBeforeServiceListsBoth() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/{id}/orders")

        assertEquals(
            listOf("com.example.app.web.TenantResolver.resolve", "com.example.app.web.OrdersService.list"),
            targets(contract)
        )
        assertEquals("com.example.app.web.TenantResolver.resolve", contract["serviceCall"]["target"].asText())
        assertEquals(lineOf("tenantResolver.resolve(request)"), contract["serviceCalls"][0]["callLine"].asInt())
        assertEquals(lineOf("ordersService.list(id, it)"), contract["serviceCalls"][1]["callLine"].asInt())
    }

    fun testExpressionBodiedHandlerHasOneCall() = runBlocking<Unit> {
        assertEquals(listOf("com.example.app.web.OrdersService.count"), targets(contractOf("/api/stores/{id}/count")))
    }

    /** Two calls of one bean method on different lines are two places a change has to be made. */
    fun testSameMethodOnTwoLinesIsListedTwice() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/{id}/compare")

        assertEquals(
            listOf("com.example.app.web.OrdersService.count", "com.example.app.web.OrdersService.count"),
            targets(contract)
        )
        assertEquals(
            listOf(lineOf("val before = ordersService.count(id)"), lineOf("val after = ordersService.count(id + 1)")),
            contract["serviceCalls"].map { it["callLine"].asInt() }
        )
    }

    fun testFrameworkAndStdlibCallsAreNeverListed() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/{id}/summary")

        assertEquals(listOf("com.example.app.web.OrdersService.count"), targets(contract))
    }

    fun testHandlerWithoutBeanCallsHasAnEmptyList() = runBlocking<Unit> {
        val contract = contractOf("/api/stores/ping")

        assertTrue("serviceCalls is always present", contract.has("serviceCalls"))
        assertEquals(0, contract["serviceCalls"].size())
        assertTrue(contract["serviceCall"].isNull)
    }

    private suspend fun contractOf(url: String): JsonNode {
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals("Exactly one contract for $url", 1, endpoints.size())
        return endpoints[0]
    }

    private fun targets(contract: JsonNode): List<String> = contract["serviceCalls"].map { it["target"].asText() }

    private fun lineOf(anchor: String): Int {
        val index = SOURCE.lines().indexOfFirst { anchor in it }
        assertTrue("Anchor '$anchor' is absent from the fixture", index >= 0)
        return index + 1
    }

    private companion object {
        val SOURCE = """
            package com.example.app.web

            import org.springframework.http.ResponseEntity
            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            @Service
            class TenantResolver {
                fun resolve(request: String): String = request
            }

            @Service
            class OrdersService {
                fun list(id: Long, tenant: String): List<String> = listOf(tenant)
                fun count(id: Long): Long = id
            }

            @RestController
            @RequestMapping("/api/stores")
            class OrdersController(private val tenantResolver: TenantResolver, private val ordersService: OrdersService) {
                @GetMapping("/{id}/orders")
                fun orders(@PathVariable id: Long, @RequestParam request: String) =
                    tenantResolver.resolve(request).let { ordersService.list(id, it) }

                @GetMapping("/{id}/count")
                fun count(@PathVariable id: Long) = ordersService.count(id)

                @GetMapping("/{id}/compare")
                fun compare(@PathVariable id: Long): Long {
                    val before = ordersService.count(id)
                    val after = ordersService.count(id + 1)
                    return after - before
                }

                @GetMapping("/{id}/summary")
                fun summary(@PathVariable id: Long): ResponseEntity<String> {
                    val label = "store-${'$'}id".trim().uppercase()
                    return ResponseEntity.ok(label + ordersService.count(id))
                }

                @GetMapping("/ping")
                fun ping() = ResponseEntity.ok("pong".trim())
            }
        """.trimIndent()
    }
}
