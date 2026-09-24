/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader

class RouterFunctionEndpointLoaderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1
    )

    fun testLiteralPathIsListed() {
        addRouterConfig(route = """route().GET("/v1/models", handler)""")

        assertEquals(listOf("/v1/models" to "GET"), webFluxEndpoints())
    }

    fun testStaticFinalConstantPathIsResolved() {
        addRouterConfig(
            route = """route().GET(MODELS, handler)""",
            constants = """private static final String MODELS = "/v1/models";"""
        )

        assertEquals(listOf("/v1/models" to "GET"), webFluxEndpoints())
    }

    /**
     * The servlet stack has its own `RouterFunctions.Builder` in `org.springframework.web.servlet.function`, which the
     * reactive-only type check used to exclude.
     */
    fun testServletBuilderRouteIsListedAsSpringMvc() {
        myFixture.addFileToProject(
            "ServletBuilderRouterConfig.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.web.servlet.function.HandlerFunction;
            import org.springframework.web.servlet.function.RouterFunction;
            import org.springframework.web.servlet.function.RouterFunctions;
            import org.springframework.web.servlet.function.ServerResponse;

            @Configuration
            public class ServletBuilderRouterConfig {

                @Bean
                public RouterFunction<ServerResponse> servletRoutes(HandlerFunction<ServerResponse> handler) {
                    return RouterFunctions.route().GET("/api/users", handler).build();
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("/api/users" to "GET"), endpointsOfType(EndpointType.SPRING_MVC))
    }

    private fun addRouterConfig(route: String, constants: String = "") {
        myFixture.addFileToProject(
            "GatewayRouterConfig.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.web.reactive.function.server.HandlerFunction;
            import org.springframework.web.reactive.function.server.RouterFunction;
            import org.springframework.web.reactive.function.server.RouterFunctions;
            import org.springframework.web.reactive.function.server.ServerResponse;

            @Configuration
            public class GatewayRouterConfig {

                $constants

                @Bean
                public RouterFunction<ServerResponse> routes(HandlerFunction<ServerResponse> handler) {
                    return RouterFunctions.$route.build();
                }
            }
            """.trimIndent()
        )
    }

    /**
     * Reads through the extension point rather than through a directly instantiated loader, so the test also proves
     * the loader is registered and reachable.
     */
    private fun webFluxEndpoints(): List<Pair<String, String>> = endpointsOfType(EndpointType.SPRING_WEBFLUX)

    private fun endpointsOfType(type: EndpointType): List<Pair<String, String>> = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(module.project).asSequence()
        .filter { it.getType() == type }
        .filter { it.isApplicable(module) }
        .flatMap { it.searchEndpoints(module) }
        .map(::toPathAndMethod)
        .sortedBy { it.first + it.second }
        .toList()

    private fun toPathAndMethod(endpoint: EndpointElement) = endpoint.path to endpoint.requestMethods.single()
}
