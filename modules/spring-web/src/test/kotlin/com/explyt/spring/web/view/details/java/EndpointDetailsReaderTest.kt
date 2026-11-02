/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.view.details.ContractRowLines.contractRowsOf
import com.explyt.spring.web.view.details.ContractRowLines.find
import com.explyt.spring.web.view.details.ContractRowLines.lines
import com.explyt.spring.web.view.details.ContractRowLines.rowOf
import com.explyt.spring.web.view.details.ContractRowLines.targetOf
import com.explyt.spring.web.view.details.EndpointDetails
import com.explyt.spring.web.view.details.EndpointDetailsReader
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointsTreeData
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

class EndpointDetailsReaderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.jacksonAnnotations_2_15_2,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/DemoApplication.java", """
            package com.example;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class DemoApplication {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/Status.java", """
            package com.example.orders;

            public enum Status { PAID, SHIPPED }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/OrderDto.java", """
            package com.example.orders;

            import com.fasterxml.jackson.annotation.JsonProperty;

            public class OrderDto {
                private Long id;
                @JsonProperty("order_status")
                private Status status;

                public Long getId() { return id; }
                public Status getStatus() { return status; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/OrderService.java", """
            package com.example.orders;

            import org.springframework.stereotype.Service;

            @Service
            public class OrderService {
                public OrderDto save(OrderDto order) { return order; }
                public OrderDto find(Long id) { return null; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/OrderController.java", """
            package com.example.orders;

            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/orders")
            public class OrderController {
                private final OrderService orderService;

                public OrderController(OrderService orderService) {
                    this.orderService = orderService;
                }

                @PostMapping(value = "/{id}", consumes = "application/json", produces = "application/json")
                public OrderDto update(
                    @PathVariable Long id,
                    @RequestParam(defaultValue = "10") int limit,
                    @RequestHeader("X-Tenant") String tenant,
                    @RequestBody OrderDto body
                ) {
                    return orderService.save(body);
                }

                @GetMapping({"/first", "/second"})
                public OrderDto twoPaths() { return orderService.find(1L); }

                @RequestMapping(value = "/both", method = {RequestMethod.GET, RequestMethod.DELETE})
                public void twoVerbs() {}
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
                "  id  PATH, java.lang.Long, required",
                "  limit  QUERY, int, optional, default 10",
                "  body  BODY, com.example.orders.OrderDto, required",
                "  X-Tenant  HEADER, java.lang.String, required",
                "Media types",
                "  consumes application/json",
                "  produces application/json",
                "Response  com.example.orders.OrderDto",
                "  id  java.lang.Long, nullability unknown",
                "  order_status  com.example.orders.Status, nullability unknown, declared as status",
                "    PAID",
                "    SHIPPED",
                "Service calls",
                "  com.example.orders.OrderService.save  line 21",
            ),
            lines(contractRowsOf(project, row))
        )
    }

    fun testEveryRowNavigatesToItsSource() {
        val rows = contractRowsOf(project, rowOf(project, "/api/orders/{id}", "POST"))

        assertEquals("update", (targetOf(rows, "POST /api/orders/{id}") as PsiMethod).name)
        assertEquals("limit", (targetOf(rows, "limit") as PsiParameter).name)
        assertEquals("body", (targetOf(rows, "body") as PsiParameter).name)
        assertEquals("OrderDto", (targetOf(rows, "Response") as PsiClass).name)
        assertEquals("status", (targetOf(rows, "order_status") as PsiField).name)
        assertEquals("save", (targetOf(rows, "com.example.orders.OrderService.save") as PsiMethod).name)
    }

    fun testUnknownNullabilityIsMarkedUncertain() {
        val rows = contractRowsOf(project, rowOf(project, "/api/orders/{id}", "POST"))

        assertTrue(find(rows, "order_status").let { it.note!!.contains("nullability unknown") && it.uncertain })
        assertFalse(find(rows, "limit").uncertain)
    }

    fun testPathsOfOneHandlerMethodResolveToTheirOwnContract() {
        val first = rowOf(project, "/api/orders/first", "GET")
        val second = rowOf(project, "/api/orders/second", "GET")
        assertEquals("Precondition: both rows share one handler", first.psiPointer.element, second.psiPointer.element)

        assertEquals("GET /api/orders/first", contractRowsOf(project, first).first().text)
        assertEquals("GET /api/orders/second", contractRowsOf(project, second).first().text)
    }

    fun testVerbsOfOneMappingShareOneContract() {
        val get = contractRowsOf(project, rowOf(project, "/api/orders/both", "GET"))
        val delete = contractRowsOf(project, rowOf(project, "/api/orders/both", "DELETE"))

        assertEquals("GET DELETE /api/orders/both", get.first().text)
        assertEquals(lines(get), lines(delete))
    }

    fun testRowWhoseEndpointIsGoneIsNotFound() {
        val row = rowOf(project, "/api/orders/{id}", "POST").copy(path = "/api/orders/removed")

        assertEquals(EndpointDetails.NotFound, EndpointDetailsReader.detailsOf(row, project))
    }

    fun testInheritedHandlerOfTwoControllersResolvesToTheControllerOfItsRow() {
        myFixture.addFileToProject(
            "com/example/status/BaseStatusController.java", """
            package com.example.status;

            import org.springframework.web.bind.annotation.GetMapping;

            public abstract class BaseStatusController {
                @GetMapping("/status")
                public String status() { return "ok"; }
            }
            """.trimIndent()
        )
        listOf("FirstStatusController", "SecondStatusController").forEach { name ->
            myFixture.addFileToProject(
                "com/example/status/$name.java", """
                package com.example.status;

                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class $name extends BaseStatusController {}
                """.trimIndent()
            )
        }
        val rows = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
            .filter { it.path == "/status" }
            .flatMap(EndpointsTreeData::rowsOf)
        assertEquals(
            "Precondition: one row per controller",
            setOf("FirstStatusController", "SecondStatusController"),
            rows.map { it.classOrFileName }.toSet()
        )
        assertEquals("Precondition: both rows share one handler", 1, rows.map { it.psiPointer.element }.distinct().size)

        rows.forEach { row ->
            assertEquals(row.classOrFileName, EndpointDetailsReader.endpointOf(row, project)?.containingClass?.name)
        }
    }

    fun testEventListenerHasNoHttpContract() {
        myFixture.addFileToProject(
            "com/example/orders/OrderEvents.java", """
            package com.example.orders;

            import org.springframework.context.event.EventListener;
            import org.springframework.stereotype.Component;

            @Component
            public class OrderEvents {
                @EventListener
                public void onOrderPlaced(String event) {}
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
