/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * An endpoint the model found must survive the contract stage.
 *
 * A functional route, an OpenAPI declaration and an Actuator endpoint without operations are all declared by
 * something other than a request-handling method, and the contract tool used to count them into `totalCount` and
 * then drop them from `endpoints` — an answer that reads as "the route does not exist" while the finder resolves
 * it (#425).
 */
class SpringBootApplicationMcpToolsetFunctionalRouteTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        addGatewayHandler()
        addCoRouterConfig()
        addAnnotatedController()
    }

    fun testFunctionalRouteIsReportedAsAPartialContract() = runBlocking<Unit> {
        val contract = contractsOf("/v1beta/models/{model}:generateContent", httpMethod = "POST").single()

        assertEquals("/v1beta/models/{model}:generateContent", contract["fullPath"].asText())
        assertEquals("PARTIAL", contract["contractStatus"].asText())
        assertEquals("GatewayProxyRouterConfig", contract["controllerClass"].asText())
        assertEquals(
            "The bean factory that registers the route is what makes it reachable",
            "gatewayProxyRoutes", contract["methodName"].asText()
        )
        assertTrue(
            "A partial contract has to say what is missing, got ${contract["contractUnavailableReason"]}",
            contract["contractUnavailableReason"].asText().contains("ServerRequest")
        )
    }

    /**
     * A caller reading `totalCount` against `endpoints.size` has no way to tell a dropped endpoint from one that
     * was never found, so the invariant is asserted on the response itself rather than on the endpoint list.
     */
    fun testEveryCountedEndpointIsReturned() = runBlocking<Unit> {
        val root = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/v1/models", projectPath = projectPath())
        )

        assertFalse("The fixture is far below the result cap", root["truncated"].asBoolean())
        assertEquals(
            "An endpoint counted into totalCount disappeared from endpoints",
            root["totalCount"].asInt(), root["endpoints"].size()
        )
        assertTrue("The fixture must produce several matches for this to mean anything", root["totalCount"].asInt() > 1)
    }

    /** The URL template is the one thing a functional route states about its request, so it is not invented. */
    fun testPathVariablesOfAFunctionalRouteAreReported() = runBlocking<Unit> {
        val contract = contractsOf("/v1beta/models/{model}:generateContent", httpMethod = "POST").single()
        val parameter = contract["parameters"].single()

        assertEquals("model", parameter["name"].asText())
        assertEquals("PATH", parameter["source"].asText())
        assertEquals("java.lang.String", parameter["type"].asText())
        assertEquals(true, parameter["required"].asBoolean())
    }

    /** The handler function is where the request shape is actually read, so the partial contract points at it. */
    fun testFunctionalRouteNamesItsHandlerFunction() = runBlocking<Unit> {
        val contract = contractsOf("/v1beta/models/{model}:generateContent", httpMethod = "POST").single()

        assertEquals("GatewayHandler.handle", contract["serviceCall"]["target"].asText())
    }

    /**
     * Matching is forgiving on purpose, so a longer unrelated path containing the pattern matches too. Ordering by
     * path specificity alone put it first, because specificity breaks ties by descending length — and the caller
     * reads its answer off the first element.
     */
    fun testExactMatchOutranksALongerPathThatMerelyContainsThePattern() = runBlocking<Unit> {
        val paths = contractsOf("/v1/models").map { it["fullPath"].asText() }

        assertTrue("The forgiving match must still find the longer paths, got $paths", paths.size > 1)
        assertEquals("/v1/models", paths.first())
    }

    /**
     * The registration signature describes the registration: its injected handler bean is not a request parameter
     * and `RouterFunction<ServerResponse>` is not a response body. Reporting them is worse than reporting nothing,
     * because a caller cannot tell an invented fact from a read one.
     */
    fun testRouteRegistrationSignatureIsNotReportedAsTheRequestShape() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "JavaRouterConfig.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.web.reactive.function.server.RouterFunction;
            import org.springframework.web.reactive.function.server.RouterFunctions;
            import org.springframework.web.reactive.function.server.ServerResponse;

            @Configuration
            public class JavaRouterConfig {

                @Bean
                public RouterFunction<ServerResponse> builderRoutes(GatewayHandler handler) {
                    return RouterFunctions.route().GET("/v1/builder/ping", request -> null).build();
                }
            }
            """.trimIndent()
        )

        val contract = contractsOf("/v1/builder/ping").single()

        assertEquals("PARTIAL", contract["contractStatus"].asText())
        assertEquals(
            "The injected handler bean is not a request parameter, got ${contract["parameters"]}",
            0, contract["parameters"].size()
        )
        assertTrue(
            "RouterFunction is the registration's type, not a response body, got ${contract["returnType"]}",
            contract["returnType"].isNull
        )
    }

    /** The contrast case: an annotated handler still yields the full contract, status and all. */
    fun testAnnotatedHandlerStillYieldsACompleteContract() = runBlocking<Unit> {
        val contract = contractsOf("/user/v1/models/popularity").single()

        assertEquals("COMPLETE", contract["contractStatus"].asText())
        assertEquals("popularity", contract["methodName"].asText())
        assertTrue(contract["contractUnavailableReason"].isNull)
        assertEquals("java.lang.String", contract["returnType"].asText())
    }

    private suspend fun contractsOf(urlPattern: String, httpMethod: String = ""): List<JsonNode> =
        mapper.readTree(
            toolset.getEndpointContract(
                urlPattern = urlPattern,
                projectPath = projectPath(),
                httpMethod = httpMethod
            )
        )["endpoints"].toList()

    private fun projectPath(): String = project.basePath ?: ""

    private fun addGatewayHandler() {
        myFixture.addFileToProject(
            "GatewayHandler.kt",
            """
            import org.springframework.stereotype.Service
            import org.springframework.web.reactive.function.server.ServerRequest
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.buildAndAwait

            @Service
            class GatewayHandler {
                suspend fun handle(request: ServerRequest): ServerResponse = ServerResponse.ok().buildAndAwait()
            }
            """.trimIndent()
        )
    }

    private fun addCoRouterConfig() {
        myFixture.addFileToProject(
            "GatewayProxyRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class GatewayProxyRouterConfig {

                @Bean
                fun gatewayProxyRoutes(handler: GatewayHandler): RouterFunction<ServerResponse> = coRouter {
                    GET("/v1/models", handler::handle)
                    POST("/v1beta/models/{model}:generateContent", handler::handle)
                }
            }
            """.trimIndent()
        )
    }

    private fun addAnnotatedController() {
        myFixture.addFileToProject(
            "UserCabinetController.kt",
            """
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("/user/v1/models")
            class UserCabinetController {

                @GetMapping("/popularity")
                fun popularity(): String = "popularity"

                @GetMapping("/usage")
                fun usage(): String = "usage"
            }
            """.trimIndent()
        )
    }
}
