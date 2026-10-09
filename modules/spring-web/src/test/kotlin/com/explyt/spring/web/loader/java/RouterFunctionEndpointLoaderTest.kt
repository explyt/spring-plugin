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
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil

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

    fun testServletPathConsumerIncludesPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testServletNestConsumerIncludesPredicatePath() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path("/a"), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testServletPathSupplierIncludesPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", () -> RouterFunctions.route().GET("/b", h::list).build()).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testServletNestedPathConsumersComposePrefixes() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> b.path("/b", c -> c.GET("/c", h::list))).build()""",
            listOf("/a/b/c" to "GET")
        )
    }

    fun testServletPathConsumerPreservesSiblingRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().GET("/x", h::x).path("/a", b -> b.GET("/b", h::list)).POST("/y", h::y).build()""",
            listOf("/a/b" to "GET", "/x" to "GET", "/y" to "POST")
        )
    }

    fun testServletSimpleBuilderRouteControl() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().GET("/x", h::x).build()""",
            listOf("/x" to "GET")
        )
    }

    fun testServletNestAcceptPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.accept(APPLICATION_JSON), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactivePathConsumerIncludesPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testReactiveNestConsumerIncludesPredicatePath() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path("/a"), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testReactivePathSupplierIncludesPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", () -> RouterFunctions.route().GET("/b", h::list).build()).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testReactiveNestedPathConsumersComposePrefixes() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> b.path("/b", c -> c.GET("/c", h::list))).build()""",
            listOf("/a/b/c" to "GET")
        )
    }

    fun testReactivePathConsumerPreservesSiblingRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().GET("/x", h::x).path("/a", b -> b.GET("/b", h::list)).POST("/y", h::y).build()""",
            listOf("/a/b" to "GET", "/x" to "GET", "/y" to "POST")
        )
    }

    fun testReactiveSimpleBuilderRouteControl() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().GET("/x", h::x).build()""",
            listOf("/x" to "GET")
        )
    }

    fun testReactiveNestAcceptPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.accept(APPLICATION_JSON), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    private fun assertBuilderRoutes(type: EndpointType, route: String, expected: List<Pair<String, String>>) {
        val functionPackage = when (type) {
            EndpointType.SPRING_MVC -> "org.springframework.web.servlet.function"
            EndpointType.SPRING_WEBFLUX -> "org.springframework.web.reactive.function.server"
            else -> error("Unsupported builder stack: $type")
        }
        val responseType = when (type) {
            EndpointType.SPRING_WEBFLUX -> "reactor.core.publisher.Mono<ServerResponse>"
            else -> "ServerResponse"
        }
        val config = myFixture.addFileToProject(
            "NestedBuilderRouterConfig.java",
            """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import $functionPackage.RequestPredicates;
            import $functionPackage.RouterFunction;
            import $functionPackage.RouterFunctions;
            import $functionPackage.ServerRequest;
            import $functionPackage.ServerResponse;
            import static org.springframework.http.MediaType.APPLICATION_JSON;

            @Configuration
            public class NestedBuilderRouterConfig {
                @Bean
                public RouterFunction<ServerResponse> routes(Handler h) {
                    return RouterFunctions.$route;
                }

                public interface Handler {
                    $responseType list(ServerRequest request);
                    $responseType x(ServerRequest request);
                    $responseType y(ServerRequest request);
                }
            }
            """.trimIndent()
        )
        val builderCalls = PsiTreeUtil.findChildrenOfType(config, PsiMethodCallExpression::class.java)
            .filter {
                it.methodExpression.referenceName in setOf("GET", "POST", "nest", "build") ||
                        it.methodExpression.referenceName == "path" && it.argumentList.expressionCount == 2
            }
        assertTrue("Fixture must contain builder calls", builderCalls.isNotEmpty())
        builderCalls.forEach { call ->
            assertEquals(
                "Builder call must resolve: ${call.text}",
                "$functionPackage.RouterFunctions.Builder",
                call.resolveMethod()?.containingClass?.qualifiedName
            )
        }

        assertEquals(expected, endpointsOfType(type))
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
