/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.view.details.ContractRowLines.contractRowsOf
import com.explyt.spring.web.view.details.ContractRowLines.lines
import com.explyt.spring.web.view.details.ContractRowLines.rowOf
import com.explyt.spring.web.view.details.ContractRowLines.targetOf
import com.explyt.spring.web.view.details.EndpointDetails
import com.explyt.spring.web.view.details.EndpointDetailsReader
import com.explyt.spring.web.loader.EndpointType
import com.intellij.psi.PsiNamedElement

class EndpointDetailsReaderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.kotlin_1_9_22,
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
            "com/example/orders/Orders.kt", """
            package com.example.orders

            import com.fasterxml.jackson.annotation.JsonProperty
            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.*

            enum class Status { PAID, SHIPPED }

            data class OrderDto(val id: Long, @get:JsonProperty("order_status") val status: Status?)

            @Service
            class OrderService {
                fun save(order: OrderDto): OrderDto = order
                fun find(id: Long): OrderDto? = null
            }

            @RestController
            @RequestMapping("/api/orders")
            class OrderController(private val orderService: OrderService) {

                @PostMapping("/{id}", consumes = ["application/json"], produces = ["application/json"])
                fun update(
                    @PathVariable id: Long,
                    @RequestParam(defaultValue = "10") limit: Int,
                    @RequestHeader("X-Tenant") tenant: String,
                    @RequestBody body: OrderDto,
                ): OrderDto = orderService.save(body)

                @GetMapping("/first", "/second")
                fun twoPaths(): OrderDto? = orderService.find(1)
            }
            """.trimIndent()
        )
    }

    fun testContractRowsDescribeParametersMediaTypesResponseAndServiceCalls() {
        val row = rowOf(project, "/api/orders/{id}", "POST")
        assertNotNull("Precondition: the row key finds its endpoint", EndpointDetailsReader.endpointOf(row, project))

        assertEquals(
            listOf(
                "POST /api/orders/{id}",
                "Parameters",
                "  id  PATH, long, required",
                "  limit  QUERY, int, optional, default 10",
                "  body  BODY, com.example.orders.OrderDto, required",
                "  X-Tenant  HEADER, java.lang.String, required",
                "Media types",
                "  consumes application/json",
                "  produces application/json",
                "Response  com.example.orders.OrderDto",
                "  id  long, non-null",
                "  order_status  com.example.orders.Status?, nullable, declared as status",
                "    PAID",
                "    SHIPPED",
                "Service calls",
                "  com.example.orders.OrderService.save  line 27",
            ),
            lines(contractRowsOf(project, row))
        )
    }

    fun testEveryRowNavigatesToItsSource() {
        val rows = contractRowsOf(project, rowOf(project, "/api/orders/{id}", "POST"))

        assertEquals("update", (targetOf(rows, "POST /api/orders/{id}") as PsiNamedElement).name)
        assertEquals("limit", (targetOf(rows, "limit") as PsiNamedElement).name)
        assertEquals("OrderDto", (targetOf(rows, "Response") as PsiNamedElement).name)
        assertEquals("status", (targetOf(rows, "order_status") as PsiNamedElement).name)
        assertEquals("save", (targetOf(rows, "com.example.orders.OrderService.save") as PsiNamedElement).name)
    }

    fun testPathsOfOneHandlerMethodResolveToTheirOwnContract() {
        val first = rowOf(project, "/api/orders/first", "GET")
        val second = rowOf(project, "/api/orders/second", "GET")
        assertEquals("Precondition: both rows share one handler", first.psiPointer.element, second.psiPointer.element)

        assertEquals("GET /api/orders/first", contractRowsOf(project, first).first().text)
        assertEquals("GET /api/orders/second", contractRowsOf(project, second).first().text)
    }

    fun testEventListenerHasNoHttpContract() {
        myFixture.addFileToProject(
            "com/example/orders/OrderEvents.kt", """
            package com.example.orders

            import org.springframework.context.event.EventListener
            import org.springframework.stereotype.Component

            @Component
            class OrderEvents {
                @EventListener
                fun onOrderPlaced(event: String) {}
            }
            """.trimIndent()
        )
        val row = rowOf(project, "onOrderPlaced", EndpointType.EVENT_LISTENERS.name)
        assertNotNull("Precondition: the row key finds its endpoint", EndpointDetailsReader.endpointOf(row, project))

        assertEquals(
            EndpointDetails.NoHttpContract(EndpointType.EVENT_LISTENERS.readable),
            EndpointDetailsReader.detailsOf(row, project)
        )
    }
}
