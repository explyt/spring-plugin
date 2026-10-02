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

    /**
     * Kotlin copies a nullable bean into a local `val` to smart-cast it; the local is the bean, so its calls are the
     * handler's service calls. The UAST visit lists an outer call before the calls in its arguments.
     */
    fun testCallsOnLocalCopiesOfNullableBeansAreListed() = runBlocking<Unit> {
        val contract = contractOf("/api/items/{id}/activity")

        assertEquals(
            listOf(
                "com.example.app.web.ItemGuard.requireVisible",
                "com.example.app.web.TenantResolver.resolve",
                "com.example.app.web.ItemStatsService.activity",
                "com.example.app.web.TenantResolver.resolveAdmin",
            ),
            targets(contract)
        )
        assertEquals(lineOf("guard.requireVisible(id"), contract["serviceCalls"][0]["callLine"].asInt())
        assertEquals(lineOf("stats.activity(id"), contract["serviceCalls"][2]["callLine"].asInt())
    }

    fun testElvisThrowAndNonNullAssertionKeepTheBean() = runBlocking<Unit> {
        assertEquals(
            listOf("com.example.app.web.ImageStore.put", "com.example.app.web.ItemStatsService.count"),
            targets(contractOf("/api/items/{id}/image"))
        )
    }

    /** A cast, in parentheses or not, changes the static type only: the local still holds the bean. */
    fun testCastKeepsTheBean() = runBlocking<Unit> {
        assertEquals(listOf("com.example.app.web.ImageStore.put"), targets(contractOf("/api/items/{id}/cast")))
    }

    /** A `var` can be pointed elsewhere and a local built from scratch is not the bean: neither is a service call. */
    fun testReassignableOrUnrelatedLocalsAreNotTheBean() = runBlocking<Unit> {
        assertEquals(emptyList<String>(), targets(contractOf("/api/items/{id}/other")))
    }

    /**
     * An elvis whose right side is another value can hold either side, so the local is not a copy of the left one;
     * only a right side that leaves the scope - `throw`, `return`, `error()` - makes the result the left side.
     */
    fun testElvisWithAFallbackValueIsNotACopy() = runBlocking<Unit> {
        assertEquals(emptyList<String>(), targets(contractOf("/api/items/{id}/fallback")))
    }

    fun testFinalJavaLocalCopyIsTheBean() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/JavaItemsController.java", JAVA_SOURCE)

        assertEquals(
            listOf("com.example.app.web.ItemStatsService.count", "com.example.app.web.ItemStatsService.count"),
            targets(contractOf("/api/java-items/{id}/copied"))
        )
    }

    fun testReassignedJavaLocalIsNotTheBean() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/JavaItemsController.java", JAVA_SOURCE)

        assertEquals(emptyList<String>(), targets(contractOf("/api/java-items/{id}/reassigned")))
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
                fun resolveAdmin(request: String): String = request
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

            @Service
            class ItemGuard {
                fun requireVisible(id: Long, tenant: String) = Unit
            }

            @Service
            class ItemStatsService {
                fun activity(id: Long, tenant: String): List<String> = listOf(tenant)
                fun count(id: Long): Long = id
            }

            @Service
            class ImageStore {
                fun put(id: Long): Long = id
            }

            class Unrelated {
                fun count(id: Long): Long = id
            }

            @RestController
            @RequestMapping("/api/items")
            class ItemsController(
                private val tenantResolver: TenantResolver,
                private val statsService: ItemStatsService?,
                private val itemGuard: ItemGuard?,
                private val imageStore: ImageStore?,
            ) {
                @GetMapping("/{id}/activity")
                fun activity(@PathVariable id: Long, @RequestParam request: String): List<String> {
                    val stats = statsService; val guard = itemGuard
                    if (stats == null || guard == null) throw IllegalStateException("disabled")
                    guard.requireVisible(id, tenantResolver.resolve(request))
                    return stats.activity(id, tenantResolver.resolveAdmin(request))
                }

                @GetMapping("/{id}/image")
                fun image(@PathVariable id: Long): Long {
                    val store = imageStore ?: throw IllegalStateException("no store")
                    val stats = statsService!!
                    return store.put(id) + stats.count(id)
                }

                @GetMapping("/{id}/cast")
                fun cast(@PathVariable id: Long): Long {
                    val store = (imageStore as ImageStore)
                    return store.put(id)
                }

                @GetMapping("/{id}/fallback")
                fun fallback(@PathVariable id: Long): Long {
                    val store = imageStore ?: ImageStore()
                    return store.put(id)
                }

                @GetMapping("/{id}/other")
                fun other(@PathVariable id: Long): Long {
                    var stats: ItemStatsService? = statsService
                    stats = ItemStatsService()
                    val fresh = Unrelated()
                    val replaced = stats.count(id)
                    return replaced + fresh.count(id)
                }
            }
        """.trimIndent()

        val JAVA_SOURCE = """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class JavaItemsController {
                private final ItemStatsService statsService;

                public JavaItemsController(ItemStatsService statsService) {
                    this.statsService = statsService;
                }

                @GetMapping("/api/java-items/{id}/copied")
                public long item(@PathVariable long id) {
                    final ItemStatsService stats = statsService;
                    ItemStatsService effectivelyFinal = statsService;
                    long declared = stats.count(id);
                    return declared + effectivelyFinal.count(id);
                }

                @GetMapping("/api/java-items/{id}/reassigned")
                public long reassigned(@PathVariable long id) {
                    ItemStatsService stats = statsService;
                    stats = new ItemStatsService();
                    return stats.count(id);
                }
            }
        """.trimIndent()
    }
}
