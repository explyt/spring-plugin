/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.trace.CallChains
import com.explyt.spring.web.view.details.ContractRowLines.find
import com.explyt.spring.web.view.details.ContractRowLines.lines
import com.explyt.spring.web.view.details.ContractRowLines.rowOf
import com.explyt.spring.web.view.details.ContractRowLines.targetOf
import com.explyt.spring.web.view.details.EndpointDetails
import com.explyt.spring.web.view.details.EndpointDetailsReader
import com.intellij.psi.PsiMethod

class CallChainRowsTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
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
            "com/example/orders/OrderService.java", """
            package com.example.orders;

            import org.springframework.scheduling.annotation.Async;
            import org.springframework.stereotype.Service;
            import org.springframework.web.client.RestTemplate;

            @Service
            public class OrderService {
                private final RestTemplate restTemplate;

                public OrderService(RestTemplate restTemplate) { this.restTemplate = restTemplate; }

                public String save(String order) {
                    audit(order);
                    return restTemplate.getForObject("http://billing/orders", String.class);
                }

                @Async
                public void audit(String order) {}
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/OrderController.java", """
            package com.example.orders;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api/orders")
            public class OrderController {
                private final OrderService service;

                public OrderController(OrderService service) { this.service = service; }

                @PostMapping
                public String create(@RequestBody String order) { return service.save(order); }
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

        assertEquals("OrderController.create(order)", rows.single().text)
        assertEquals("CONTROLLER", rows.single().note)
        assertEquals(text, "SERVICE, called at line 16", find(rows, "OrderService.save(order)").note)
        assertEquals(text, "external, called at line 15", find(rows, "RestTemplate.getForObject").note)
        val audit = find(rows, "OrderService.audit(order)")
        assertEquals(text, "SERVICE, called at line 14", audit.note)
        assertTrue(text, audit.children.any { it.text == "@Async" })
        assertTrue(text, audit.children.any { it.uncertain && it.text.contains("Called from the same class, so the proxy does not apply to this call") })
        assertEquals("save", (targetOf(rows, "OrderService.save(order)") as PsiMethod).name)
        assertEquals("audit", (targetOf(rows, "OrderService.audit(order)") as PsiMethod).name)
    }

    fun testFunctionalRouteStartsAtItsHandlerMethod() {
        myFixture.addFileToProject(
            "com/example/orders/OrderHandler.java", """
            package com.example.orders;

            import org.springframework.stereotype.Component;
            import org.springframework.web.servlet.function.ServerRequest;
            import org.springframework.web.servlet.function.ServerResponse;

            @Component
            public class OrderHandler {
                public ServerResponse list(ServerRequest request) { return ServerResponse.ok().build(); }
                public ServerResponse create(ServerRequest request) { return ServerResponse.ok().build(); }
                public ServerResponse other(ServerRequest request) { return ServerResponse.ok().build(); }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/orders/OrderRoutes.java", """
            package com.example.orders;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.web.servlet.function.RouterFunction;
            import org.springframework.web.servlet.function.RouterFunctions;
            import org.springframework.web.servlet.function.ServerResponse;

            @Configuration
            public class OrderRoutes {
                @Bean
                public RouterFunction<ServerResponse> orderRoutes(OrderHandler handler) {
                    return RouterFunctions.route()
                        .GET("/fn/other", handler::other)
                        .GET("/fn/orders", handler::list)
                        .POST("/fn/orders", handler::create)
                        .build();
                }
            }
            """.trimIndent()
        )

        for ((path, verb, handler) in listOf(
            Triple("/fn/other", "GET", "other"),
            Triple("/fn/orders", "GET", "list"),
            Triple("/fn/orders", "POST", "create")
        )) {
            val endpoint = EndpointDetailsReader.endpointOf(rowOf(project, path, verb), project)
            assertNotNull("the functional route must be listed as an endpoint", endpoint)
            val start = CallChains.startOf(endpoint!!)
            assertNotNull("the route must be recognised as a functional route", start?.route)
            assertEquals(handler, start?.method?.name)
            assertEquals("$verb $path", start?.route?.route)
            val rows = (EndpointDetailsReader.callChainOf(rowOf(project, path, verb), project) as EndpointDetails.Contract).rows
            assertEquals("OrderHandler.$handler(request)", rows.single().text)
            assertEquals("OrderHandler", start?.method?.containingClass?.name)
        }
    }

    fun testUnmatchedFunctionalRouteHasNoCallChainStart() {
        myFixture.addFileToProject(
            "com/example/orders/HealthRoutes.java", """
            package com.example.orders;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.web.servlet.function.HandlerFunction;
            import org.springframework.web.servlet.function.RouterFunction;
            import org.springframework.web.servlet.function.RouterFunctions;
            import org.springframework.web.servlet.function.ServerResponse;

            @Configuration
            public class HealthRoutes {
                private final HandlerFunction<ServerResponse> health = request -> ServerResponse.ok().build();

                @Bean
                public RouterFunction<ServerResponse> healthRoutes() {
                    return RouterFunctions.route()
                        .GET("/fn/health", health)
                        .build();
                }
            }
            """.trimIndent()
        )

        val row = rowOf(project, "/fn/health", "GET")
        val endpoint = EndpointDetailsReader.endpointOf(row, project)
        assertNotNull("the functional route must be listed as an endpoint", endpoint)
        assertNull("the router factory must not be traced as the route's start", CallChains.startOf(endpoint!!))
        assertEmpty((EndpointDetailsReader.callChainOf(row, project) as EndpointDetails.Contract).rows)
    }
}
