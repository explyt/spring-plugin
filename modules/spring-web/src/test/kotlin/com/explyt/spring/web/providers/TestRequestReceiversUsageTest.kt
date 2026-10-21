/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

/**
 * Which test requests count as usages of an endpoint, by the client that sends them.
 *
 * MockMvc dispatches inside the JVM, so the host a test names is part of the request - a tenant or a brand resolved
 * from it - and any host reaches the controller under test. A real HTTP client goes over the network, so only a request
 * to this machine reaches the project; one to another host is a call to another service.
 */
class TestRequestReceiversUsageTest : ExplytKotlinLightTestCase() {

    override val realJdk = true

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_1_4,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootTest_3_2_3,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/ItemController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/items")
            public class ItemController {
                @GetMapping("/{id}") public String get(@PathVariable String id) { return id; }
                @PostMapping public String create(@RequestBody String body) { return body; }
            }
            """.trimIndent()
        )
    }

    /** The host of a MockMvc request becomes its server name; a multi-brand test sets it on purpose. */
    fun testMockMvcRequestToAnyHostReachesTheControllerUnderTest() {
        myFixture.configureByText(
            "ItemControllerTest.kt", """
            import java.net.URI
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class ItemControllerTest {
                fun brandA(id: Long) { get(URI.create("http://brand-a.example/api/items/${'$'}id?window=7d")) }
                fun brandB(id: Long) { get(URI.create("http://brand-b.example/api/items/${'$'}id")) }
                fun brandAsString() { get("https://brand-a.example/api/items/{id}", 42) }
                fun relative(id: Long) { get("/api/items/${'$'}id") }
                fun otherRoute(id: Long) { get(URI.create("http://brand-a.example/other/${'$'}id")) }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("brandA", "brandAsString", "brandB", "relative"),
            callersOf(EndpointUsageSearcher.findMockMvcEndpointUsage("/api/items/{id}", listOf("GET"), module))
        )
    }

    /** A `RANDOM_PORT` test calling its own server through `java.net.http` is exactly the localhost case. */
    fun testJavaHttpClientRequestToThisMachineIsAUsage() {
        assertNotNull(
            "Precondition: the JDK of the fixture provides java.net.http",
            JavaPsiFacade.getInstance(project).findClass("java.net.http.HttpRequest", module.moduleWithLibrariesScope)
        )
        myFixture.configureByText(
            "ItemContainerTest.kt", """
            import java.net.URI
            import java.net.http.HttpRequest

            class ItemContainerTest {
                private val port = 8080

                fun builderUri(id: Long) =
                    HttpRequest.newBuilder().uri(URI.create("http://localhost:${'$'}port/api/items/${'$'}id")).GET().build()

                fun newBuilderWithUri(id: Long) =
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:${'$'}port/api/items/${'$'}id")).build()

                fun otherHost(id: Long) =
                    HttpRequest.newBuilder().uri(URI.create("http://payments.example.com/api/items/${'$'}id")).build()

                fun otherMethod(id: Long) =
                    HttpRequest.newBuilder().uri(URI.create("http://localhost:${'$'}port/api/items/${'$'}id"))
                        .DELETE().build()

                fun unfinished() = HttpRequest.newBuilder().uri(URI.create("http://localhost:${'$'}port/api/items"))
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("builderUri", "newBuilderWithUri"),
            callersOf(EndpointUsageSearcher.findHttpClientEndpointUsage("/api/items/{id}", listOf("GET"), module))
        )
        assertEquals(
            "A builder left unfinished names no method, so no method filter excludes it",
            listOf("unfinished"),
            callersOf(EndpointUsageSearcher.findHttpClientEndpointUsage("/api/items", listOf("POST"), module))
        )
    }

    fun testRestTemplateAndTestRestTemplateRequestsToThisMachineAreUsages() {
        myFixture.configureByText(
            "ItemRestTest.kt", """
            import org.springframework.boot.test.web.client.TestRestTemplate
            import org.springframework.http.HttpMethod
            import org.springframework.web.client.RestOperations
            import org.springframework.web.client.RestTemplate

            class ItemRestTest {
                fun testRestTemplate(rest: TestRestTemplate, id: Long) {
                    rest.getForObject("http://localhost:8080/api/items/${'$'}id", String::class.java)
                }
                fun restTemplate(rest: RestTemplate, id: Long) {
                    rest.getForEntity("/api/items/${'$'}id", String::class.java)
                }
                fun restOperationsExchange(rest: RestOperations, id: Long) {
                    rest.exchange("http://localhost:8080/api/items/${'$'}id", HttpMethod.GET, null, String::class.java)
                }
                fun otherHost(rest: TestRestTemplate, id: Long) {
                    rest.getForObject("http://payments.example.com/api/items/${'$'}id", String::class.java)
                }
                fun otherMethod(rest: RestTemplate, id: Long) {
                    rest.delete("http://localhost:8080/api/items/${'$'}id")
                }
                fun otherMethodExchange(rest: RestOperations, id: Long) {
                    rest.exchange("http://localhost:8080/api/items/${'$'}id", HttpMethod.DELETE, null, String::class.java)
                }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("restOperationsExchange", "restTemplate", "testRestTemplate"),
            callersOf(EndpointUsageSearcher.findHttpClientEndpointUsage("/api/items/{id}", listOf("GET"), module))
        )
    }

    fun testRestClientRequestToThisMachineIsAUsage() {
        myFixture.configureByText(
            "ItemRestClientTest.kt", """
            import org.springframework.http.HttpMethod
            import org.springframework.web.client.RestClient

            class ItemRestClientTest {
                fun get(client: RestClient, id: Long) { client.get().uri("http://localhost:8080/api/items/${'$'}id").retrieve() }
                fun method(client: RestClient, id: Long) { client.method(HttpMethod.GET).uri("/api/items/${'$'}id").retrieve() }
                fun post(client: RestClient) { client.post().uri("http://localhost:8080/api/items").retrieve() }
                fun otherMethod(client: RestClient, id: Long) { client.delete().uri("http://localhost:8080/api/items/${'$'}id").retrieve() }
                fun otherHost(client: RestClient, id: Long) { client.get().uri("http://payments.example.com/api/items/${'$'}id") }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("get", "method"),
            callersOf(EndpointUsageSearcher.findHttpClientEndpointUsage("/api/items/{id}", listOf("GET"), module))
        )
        assertEquals(
            listOf("post"),
            callersOf(EndpointUsageSearcher.findHttpClientEndpointUsage("/api/items", listOf("POST"), module))
        )
    }

    /**
     * A WebTestClient's URL keeps the host gate: whether it is bound to a controller or to a running server is decided
     * where it is built, often by `@AutoConfigureWebTestClient` on an injected field, not in the expression sending it.
     */
    fun testWebTestClientKeepsTheHostGate() {
        myFixture.configureByText(
            "ItemWebTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient

            class ItemWebTest {
                fun localhost(client: WebTestClient, id: Long) { client.get().uri("http://localhost:8080/api/items/${'$'}id") }
                fun otherHost(client: WebTestClient, id: Long) { client.get().uri("http://payments.example.com/api/items/${'$'}id") }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("localhost"),
            callersOf(EndpointUsageSearcher.findWebTestClientEndpointUsage("/api/items/{id}", listOf("GET"), module))
        )
    }

    /** Strings that look like paths but are not sent anywhere - input data of a classifier - are not requests. */
    fun testPathLikeTestDataIsNotARequest() {
        myFixture.configureByText(
            "PathCategoryTest.kt", """
            import java.net.URI

            class PathCategoryTest {
                fun classify(path: String) = path.length

                fun categories() = listOf("/api/items/6f0c", "/api/items/42", "http://localhost/api/items/1").map(::classify)
                fun uri() = URI.create("http://localhost:8080/api/items/7")
            }
            """.trimIndent()
        )

        assertEquals(emptyList<String>(), callersOf(EndpointUsageSearcher.findTestRequestUsage("/api/items/{id}", listOf("GET"), module)))
    }

    /** The endpoint gutter lists every client the requests above came from, each request once. */
    fun testEveryClientIsListedOnce() {
        myFixture.configureByText(
            "ItemAllClientsTest.kt", """
            import java.net.URI
            import java.net.http.HttpRequest
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class ItemAllClientsTest {
                fun mockMvc(id: Long) { get(URI.create("http://brand-a.example/api/items/${'$'}id")) }
                fun javaHttp(id: Long) = HttpRequest.newBuilder(URI.create("http://localhost/api/items/${'$'}id")).build()
            }
            """.trimIndent()
        )

        val usages = EndpointUsageSearcher.findTestRequestUsage("/api/items/{id}", listOf("GET"), module)
        assertEquals(listOf("javaHttp", "mockMvc"), callersOf(usages))
        assertEquals(usages.size, usages.distinct().size)
    }

    fun testMockMvcRequestsOfEveryVerbReachAnAnyMethodMapping() {
        val endpoint = anyMethodEndpoint()
        myFixture.configureByText(
            "AnyMockMvcTest.kt", """
            import org.springframework.http.HttpMethod
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

            class AnyMockMvcTest {
                fun getting() { get("/any") }
                fun posting() { post("/any") }
                fun optionsRequest() { options("/any") }
                fun requestByMethod() { request(HttpMethod.PUT, "/any") }
                fun otherRoute() { get("/other") }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("getting", "optionsRequest", "posting", "requestByMethod"),
            callersOf(EndpointUsageSearcher.findMockMvcEndpointUsage(endpoint.path, endpoint.requestMethods, module))
        )
    }

    fun testWebTestClientRequestsOfEveryVerbReachAnAnyMethodMapping() {
        val endpoint = anyMethodEndpoint()
        myFixture.configureByText(
            "AnyWebTest.kt", """
            import org.springframework.http.HttpMethod
            import org.springframework.test.web.reactive.server.WebTestClient

            class AnyWebTest {
                fun getting(client: WebTestClient) { client.get().uri("/any") }
                fun posting(client: WebTestClient) { client.post().uri("/any") }
                fun byMethod(client: WebTestClient) { client.method(HttpMethod.PATCH).uri("/any") }
                fun otherRoute(client: WebTestClient) { client.get().uri("/other") }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("byMethod", "getting", "posting"),
            callersOf(EndpointUsageSearcher.findWebTestClientEndpointUsage(endpoint.path, endpoint.requestMethods, module))
        )
    }

    fun testNetworkClientRequestsOfEveryVerbStillReachAnAnyMethodMapping() {
        val endpoint = anyMethodEndpoint()
        myFixture.configureByText(
            "AnyRestClientTest.kt", """
            import org.springframework.web.client.RestClient

            class AnyRestClientTest {
                fun getting(client: RestClient) { client.get().uri("http://localhost:8080/any").retrieve() }
                fun posting(client: RestClient) { client.post().uri("http://localhost:8080/any").retrieve() }
                fun otherHost(client: RestClient) { client.get().uri("http://payments.example.com/any").retrieve() }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("getting", "posting"),
            callersOf(EndpointUsageSearcher.findHttpClientEndpointUsage(endpoint.path, endpoint.requestMethods, module))
        )
    }

    fun testEveryClientOfEveryVerbIsListedForAnAnyMethodMapping() {
        val endpoint = anyMethodEndpoint()
        myFixture.configureByText(
            "AnyAllClientsTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
            import org.springframework.web.client.RestClient

            class AnyAllClientsTest {
                fun mockMvc() { post("/any") }
                fun webTestClient(client: WebTestClient) { client.post().uri("/any") }
                fun restClient(client: RestClient) { client.post().uri("http://localhost/any").retrieve() }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("mockMvc", "restClient", "webTestClient"),
            callersOf(EndpointUsageSearcher.findTestRequestUsage(endpoint.path, endpoint.requestMethods, module))
        )
    }

    fun testRequestsOfAnotherVerbDoNotReachAGetMapping() {
        myFixture.configureByText(
            "ItemVerbTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

            class ItemVerbTest {
                fun mockMvcGet() { get("/api/items/1") }
                fun mockMvcPost() { post("/api/items/1") }
                fun webGet(client: WebTestClient) { client.get().uri("/api/items/1") }
                fun webPost(client: WebTestClient) { client.post().uri("/api/items/1") }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("mockMvcGet", "webGet"),
            callersOf(EndpointUsageSearcher.findTestRequestUsage("/api/items/{id}", listOf("GET"), module))
        )
    }

    fun testRequestsOfEveryVerbReachAnAnyMethodRouterPath() {
        myFixture.addFileToProject(
            "com/example/AnyRoutes.kt", """
            package com.example

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerRequest
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.buildAndAwait
            import org.springframework.web.reactive.function.server.coRouter

            class AnyHandler {
                suspend fun handle(request: ServerRequest): ServerResponse = ServerResponse.ok().buildAndAwait()
            }

            @Configuration
            class AnyRoutes {
                @Bean
                fun routes(handler: AnyHandler): RouterFunction<ServerResponse> = coRouter {
                    path("/routed", handler::handle)
                }
            }
            """.trimIndent()
        )
        val endpoint = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .single { it.path == "/routed" }
        assertEquals(emptyList<String>(), endpoint.requestMethods)
        myFixture.configureByText(
            "AnyRoutedTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

            class AnyRoutedTest {
                fun mockMvcGet() { get("/routed") }
                fun mockMvcPost() { post("/routed") }
                fun webGet(client: WebTestClient) { client.get().uri("/routed") }
                fun webPost(client: WebTestClient) { client.post().uri("/routed") }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("mockMvcGet", "mockMvcPost", "webGet", "webPost"),
            callersOf(EndpointUsageSearcher.findTestRequestUsage(endpoint.path, endpoint.requestMethods, module))
        )
    }

    fun testLowercaseDeclaredMethodIsMatchedRegardlessOfCase() {
        myFixture.addFileToProject(
            "com/example/LowerClient.java", """
            package com.example;

            import org.springframework.web.service.annotation.HttpExchange;

            public interface LowerClient {
                @HttpExchange(value = "/lower", method = "get")
                String lower();
            }
            """.trimIndent()
        )
        val endpoint = endpointsOf("LowerClient").single()
        assertEquals("/lower", endpoint.path)
        assertEquals(listOf("get"), endpoint.requestMethods)
        myFixture.configureByText(
            "LowerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

            class LowerTest {
                fun getting() { get("/lower") }
                fun posting() { post("/lower") }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("getting"),
            callersOf(EndpointUsageSearcher.findMockMvcEndpointUsage(endpoint.path, endpoint.requestMethods, module))
        )
    }

    private fun anyMethodEndpoint(): EndpointElement {
        myFixture.addFileToProject(
            "com/example/AnyController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class AnyController {
                @RequestMapping("/any") public String any() { return "any"; }
            }
            """.trimIndent()
        )
        val endpoint = endpointsOf("AnyController").single()
        assertEquals("/any", endpoint.path)
        assertEquals(emptyList<String>(), endpoint.requestMethods)
        return endpoint
    }

    private fun endpointsOf(className: String): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.containingClass?.name == className }

    private fun callersOf(usages: List<PsiElement>): List<String> = usages.map(::enclosingMethodName).sorted()

    private fun enclosingMethodName(element: PsiElement): String =
        element.toUElement()?.getParentOfType<UMethod>()?.name
            ?: PsiTreeUtil.getParentOfType(element, PsiNamedElement::class.java)?.name
            ?: "?"
}
