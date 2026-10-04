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
 * `explyt_get_spring_endpoint_contract` for a project whose own package is under `org.springframework.` - a sample
 * application, a fork, or one of Spring's own repositories.
 *
 * Framework code is told apart from the project by where it is declared, not by its package: a project class is never
 * unwrapped as a container and a project bean is never skipped as framework code. A Java class is described by what
 * Jackson writes, which is its public getters and public fields, not every field it stores.
 */
class SpringBootApplicationMcpToolsetFrameworkPackageTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.jacksonAnnotations_2_15_2,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        SOURCES.forEach { (path, text) -> myFixture.addFileToProject(path, text.trimIndent()) }
    }

    /** A project DTO under `org.springframework.` was unwrapped as if it were a framework container, giving `null`. */
    fun testProjectClassUnderTheSpringPackageIsDescribedByItsGetters() = runBlocking<Unit> {
        val schema = responseSchema("/vets", "GET")

        assertEquals("$SHOP.vet.Vets", schema["className"].asText())
        assertEquals(
            "Jackson writes the getter, not the private field behind it",
            listOf("vetList"), names(schema)
        )
        assertEquals(
            "A list reaches its element type",
            "$SHOP.vet.Vet", field(schema, "vetList")["nested"]["className"].asText()
        )
    }

    fun testPrivateFieldWithoutAGetterIsNotWritten() = runBlocking<Unit> {
        val schema = responseSchema("/orders/one", "GET")

        assertEquals("ResponseEntity still unwraps to its body", "$SHOP.order.OrderDto", schema["className"].asText())
        assertEquals(listOf("id", "note"), names(schema))
    }

    fun testJsonPropertyOnAJavaGetterRenamesTheProperty() = runBlocking<Unit> {
        val fields = responseSchema("/receipt", "GET")["fields"]

        assertEquals(listOf("order_id"), fields.map { it["name"].asText() })
        assertEquals("orderId", fields.single()["declaredName"].asText())
    }

    fun testCallOnAProjectBeanUnderTheSpringPackageIsAServiceCall() = runBlocking<Unit> {
        val contract = contractOf("/orders", "POST")

        assertEquals(
            listOf("$SHOP.order.OrderStore.save"),
            contract["serviceCalls"].map { it["target"].asText() }
        )
    }

    /** The collection rule is unchanged: a JDK list still reaches its element, outside the Spring package too. */
    fun testListOfAPlainProjectDtoStillReachesTheElement() = runBlocking<Unit> {
        val schema = responseSchema("/plain/items", "GET")

        assertEquals("com.example.plain.PlainItem", schema["className"].asText())
        assertEquals(listOf("id"), names(schema))
    }

    /**
     * A Java bean is what Jackson writes: its getters under Jackson's default naming - a leading run of capitals is
     * lower-cased as a whole, `getURL` is `url` - and not the private fields behind them.
     */
    fun testJavaBeanOutsideTheSpringPackageIsDescribedByItsGetters() = runBlocking<Unit> {
        val schema = responseSchema("/plain/report", "GET")

        assertEquals(listOf("rowList", "url"), names(schema))
    }

    /** JDK value types keep no expanded schema: they are not containers and not DTOs. */
    fun testJdkValueTypesStayUnexpanded() = runBlocking<Unit> {
        assertTrue(responseSchemaOrNull("/orders/label", "GET").isNull)
        assertTrue(responseSchemaOrNull("/orders/since", "GET").isNull)
    }

    private suspend fun contractOf(url: String, method: String): JsonNode {
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = method)
        )["endpoints"]
        assertEquals("Exactly one contract for $method $url", 1, endpoints.size())
        return endpoints[0]
    }

    private suspend fun responseSchemaOrNull(url: String, method: String): JsonNode = contractOf(url, method)["responseSchema"]

    private suspend fun responseSchema(url: String, method: String): JsonNode {
        val schema = responseSchemaOrNull(url, method)
        assertFalse("Expected a response schema for $method $url", schema.isNull)
        return schema
    }

    private fun names(schema: JsonNode): List<String> = schema["fields"].map { it["name"].asText() }

    private fun field(schema: JsonNode, name: String): JsonNode = schema["fields"].single { it["name"].asText() == name }

    private companion object {
        const val SHOP = "org.springframework.samples.shop"

        val SOURCES = mapOf(
            "org/springframework/samples/shop/ShopApplication.java" to """
                package org.springframework.samples.shop;

                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class ShopApplication {
                }
            """,
            "org/springframework/samples/shop/vet/Vet.java" to """
                package org.springframework.samples.shop.vet;

                public class Vet {
                    private String name;

                    public String getName() { return name; }
                }
            """,
            "org/springframework/samples/shop/vet/Vets.java" to """
                package org.springframework.samples.shop.vet;

                import java.util.ArrayList;
                import java.util.List;

                public class Vets {
                    private List<Vet> vets;

                    public List<Vet> getVetList() {
                        if (vets == null) vets = new ArrayList<>();
                        return vets;
                    }
                }
            """,
            "org/springframework/samples/shop/order/OrderDto.java" to """
                package org.springframework.samples.shop.order;

                public class OrderDto {
                    private long id;
                    private String note;
                    private String secret;

                    public long getId() { return id; }
                    public String getNote() { return note; }
                }
            """,
            "org/springframework/samples/shop/order/Receipt.java" to """
                package org.springframework.samples.shop.order;

                import com.fasterxml.jackson.annotation.JsonProperty;

                public class Receipt {
                    private long orderId;

                    @JsonProperty("order_id")
                    public long getOrderId() { return orderId; }
                }
            """,
            "org/springframework/samples/shop/order/OrderStore.java" to """
                package org.springframework.samples.shop.order;

                import org.springframework.stereotype.Repository;

                @Repository
                public class OrderStore {
                    public OrderDto save(OrderDto order) { return order; }
                }
            """,
            "org/springframework/samples/shop/web/ShopController.java" to """
                package org.springframework.samples.shop.web;

                import java.time.Instant;
                import org.springframework.http.ResponseEntity;
                import org.springframework.samples.shop.order.OrderDto;
                import org.springframework.samples.shop.order.OrderStore;
                import org.springframework.samples.shop.order.Receipt;
                import org.springframework.samples.shop.vet.Vets;
                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.ResponseBody;

                @Controller
                public class ShopController {
                    private final OrderStore orders;

                    public ShopController(OrderStore orders) { this.orders = orders; }

                    @GetMapping("/vets")
                    public @ResponseBody Vets vets() { return new Vets(); }

                    @PostMapping("/orders")
                    public @ResponseBody OrderDto save(@RequestBody OrderDto order) { return orders.save(order); }

                    @GetMapping("/orders/one")
                    public ResponseEntity<OrderDto> one() { return ResponseEntity.ok(new OrderDto()); }

                    @GetMapping("/orders/label")
                    public @ResponseBody String label() { return "order"; }

                    @GetMapping("/orders/since")
                    public @ResponseBody Instant since() { return Instant.now(); }

                    @GetMapping("/receipt")
                    public @ResponseBody Receipt receipt() { return new Receipt(); }
                }
            """,
            "com/example/plain/PlainItem.java" to """
                package com.example.plain;

                public class PlainItem {
                    public long id;
                }
            """,
            "com/example/plain/PlainReport.java" to """
                package com.example.plain;

                import java.util.List;

                public class PlainReport {
                    private List<PlainItem> rows;
                    private String secret;

                    public List<PlainItem> getRowList() { return rows; }
                    public String getURL() { return ""; }
                }
            """,
            "com/example/plain/PlainController.java" to """
                package com.example.plain;

                import java.util.List;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class PlainController {
                    @GetMapping("/plain/items")
                    public List<PlainItem> items() { return List.of(); }

                    @GetMapping("/plain/report")
                    public PlainReport report() { return new PlainReport(); }
                }
            """,
        )
    }
}
