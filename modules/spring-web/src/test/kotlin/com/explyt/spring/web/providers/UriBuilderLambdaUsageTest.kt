/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

/**
 * A test request whose URL is built by a `Function<UriBuilder, URI>`: `uri { it.path("/api/orders/status").build() }`.
 *
 * The path is the argument of the builder's `path(...)` call inside the lambda; query parameters do not take part in
 * dispatching, so they are ignored. A lambda that names no path addresses no endpoint.
 */
class UriBuilderLambdaUsageTest : ExplytKotlinLightTestCase() {

    override val realJdk = true

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_1_4,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/OrderController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/orders")
            public class OrderController {
                @GetMapping("/status") public String status(@RequestParam String id) { return id; }
                @GetMapping("/{id}/lines/{line}") public String line(@PathVariable String id, @PathVariable String line) { return id; }
            }
            """.trimIndent()
        )
    }

    fun testWebTestClientUriBuilderLambdaAddressesTheEndpoint() {
        myFixture.configureByText(
            "OrderWebTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient

            class OrderWebTest {
                fun status(client: WebTestClient, id: String) {
                    client.get().uri { it.path("/api/orders/status").queryParam("id", id).build() }.exchange()
                }
                fun otherRoute(client: WebTestClient) { client.get().uri { it.path("/other").build() }.exchange() }
                fun noPath(client: WebTestClient, id: String) { client.get().uri { it.queryParam("id", id).build() }.exchange() }
            }
            """.trimIndent()
        )

        assertEquals(listOf("status"), webTestClientCallers("/api/orders/status"))
        assertEquals(
            "A builder without a path names no route, not the root one",
            emptyList<String>(), webTestClientCallers("/")
        )
    }

    fun testJavaWebTestClientUriBuilderLambdaAddressesTheEndpoint() {
        myFixture.configureByText(
            "OrderWebJavaTest.java", """
            import org.springframework.test.web.reactive.server.WebTestClient;

            class OrderWebJavaTest {
                void status(WebTestClient client) {
                    client.get().uri(builder -> builder.path("/api/orders/status").build()).exchange();
                }
                void otherRoute(WebTestClient client) {
                    client.get().uri(builder -> builder.path("/other").build()).exchange();
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("status"), webTestClientCallers("/api/orders/status"))
    }

    /** `path(...)` appends and `pathSegment(...)` adds one segment per argument, as `UriComponentsBuilder` does. */
    fun testPathAndPathSegmentsAreJoined() {
        myFixture.configureByText(
            "OrderLineWebTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient

            class OrderLineWebTest {
                fun line(client: WebTestClient, id: String, line: Int) {
                    client.get().uri { it.path("/api/orders").pathSegment(id, "lines", line.toString()).build() }.exchange()
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("line"), webTestClientCallers("/api/orders/{id}/lines/{line}"))
    }

    fun testRestClientUriBuilderLambdaToThisMachineIsAUsage() {
        myFixture.configureByText(
            "OrderRestClientTest.kt", """
            import org.springframework.web.client.RestClient

            class OrderRestClientTest {
                fun status(client: RestClient, id: String) {
                    client.get().uri { it.path("/api/orders/status").queryParam("id", id).build() }.retrieve()
                }
                fun noPath(client: RestClient, id: String) { client.get().uri { it.queryParam("id", id).build() }.retrieve() }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("status"),
            callersOf(EndpointUsageSearcher.findHttpClientEndpointUsage("/api/orders/status", listOf("GET"), module))
        )
    }

    private fun webTestClientCallers(endpoint: String): List<String> =
        callersOf(EndpointUsageSearcher.findWebTestClientEndpointUsage(endpoint, listOf("GET"), module))

    private fun callersOf(usages: List<PsiElement>): List<String> = usages.map(::enclosingMethodName).sorted()

    private fun enclosingMethodName(element: PsiElement): String =
        element.toUElement()?.getParentOfType<UMethod>()?.name
            ?: PsiTreeUtil.getParentOfType(element, PsiNamedElement::class.java)?.name
            ?: "?"
}
