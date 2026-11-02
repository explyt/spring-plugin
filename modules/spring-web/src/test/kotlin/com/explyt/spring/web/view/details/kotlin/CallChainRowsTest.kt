/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.trace.CallChains
import com.explyt.spring.web.view.details.ContractRowLines.find
import com.explyt.spring.web.view.details.ContractRowLines.lines
import com.explyt.spring.web.view.details.ContractRowLines.rowOf
import com.explyt.spring.web.view.details.ContractRowLines.targetOf
import com.explyt.spring.web.view.details.EndpointDetails
import com.explyt.spring.web.view.details.EndpointDetailsReader
import org.jetbrains.kotlin.psi.KtNamedFunction

class CallChainRowsTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/DemoApplication.kt", """
            package com.example

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class DemoApplication
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/OrderService.kt", """
            package com.example.orders

            import org.springframework.scheduling.annotation.Async
            import org.springframework.stereotype.Service
            import org.springframework.web.client.RestTemplate

            @Service
            class OrderService(private val restTemplate: RestTemplate) {
                fun save(order: String): String? {
                    audit(order)
                    return restTemplate.getForObject("http://billing/orders", String::class.java)
                }

                @Async
                fun audit(order: String) {}
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/OrderController.kt", """
            package com.example.orders

            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RequestBody
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("/api/orders")
            class OrderController(private val service: OrderService) {
                @PostMapping
                fun create(@RequestBody order: String): String? = service.save(order)
            }
            """.trimIndent()
        )
    }

    fun testCallChainListsLayersExternalCallsAndSelfInvocation() {
        val row = rowOf(project, "/api/orders", "POST")
        val endpoint = EndpointDetailsReader.endpointOf(row, project)
        assertNotNull("the row must resolve to its endpoint", endpoint)
        assertEquals("create", CallChains.startOf(endpoint!!)?.method?.name)

        val rows = (EndpointDetailsReader.callChainOf(row, project) as EndpointDetails.Contract).rows
        val text = lines(rows).joinToString("\n")

        assertEquals(text, "OrderController.create(order)", rows.single().text)
        assertEquals(text, "CONTROLLER", rows.single().note)
        assertEquals(text, "SERVICE, called at line 12", find(rows, "OrderService.save(order)").note)
        assertEquals(text, "external, called at line 11", find(rows, "RestTemplate.getForObject").note)
        val audit = find(rows, "OrderService.audit(order)")
        assertEquals(text, "SERVICE, called at line 10", audit.note)
        assertTrue(text, audit.children.any { it.text == "@Async" })
        assertTrue(text, audit.children.any { it.uncertain && it.text.contains("Called from the same class, so the proxy does not apply to this call") })
        assertEquals("save", (targetOf(rows, "OrderService.save(order)") as KtNamedFunction).name)
        assertEquals("audit", (targetOf(rows, "OrderService.audit(order)") as KtNamedFunction).name)
    }
    fun testRouterReferenceStartsAtItsOwnHandler() {
        myFixture.addFileToProject(
            "com/example/orders/OrderRoutes.kt", """"
            package com.example.orders

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.servlet.function.RouterFunction
            import org.springframework.web.servlet.function.ServerRequest
            import org.springframework.web.servlet.function.ServerResponse
            import org.springframework.web.servlet.function.router

            class OrderHandler {
                fun list(request: ServerRequest): ServerResponse = ServerResponse.ok().build()
                fun create(request: ServerRequest): ServerResponse = ServerResponse.ok().build()
            }

            @Configuration
            class OrderRoutes {
                @Bean
                fun routes(handler: OrderHandler): RouterFunction<ServerResponse> = router {
                    GET("/fn/orders", handler::list)
                    POST("/fn/orders", handler::create)
                }
            }
            """.trimIndent()
        )
        for ((verb, handler) in listOf("GET" to "list", "POST" to "create")) {
            val row = rowOf(project, "/fn/orders", verb)
            val endpoint = EndpointDetailsReader.endpointOf(row, project)
            assertNotNull("the Kotlin route must be listed", endpoint)
            val start = CallChains.startOf(endpoint!!)
            assertNotNull("the Kotlin route must retain its route call", start?.route)
            assertEquals(handler, start?.method?.name)
            assertEquals("$verb /fn/orders", start?.route?.route)
            val rows = (EndpointDetailsReader.callChainOf(row, project) as EndpointDetails.Contract).rows
            assertEquals("OrderHandler.$handler(request)", rows.single().text)
        }
    }

}
