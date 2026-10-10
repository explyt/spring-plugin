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

    fun testServletConstantPathPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path(API, b -> b.GET("/b", h::list)).build()""",
            listOf("/api/b" to "GET"), members = """private static final String API = "/api";"""
        )
    }

    fun testReactiveConstantPathPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path(API, b -> b.GET("/b", h::list)).build()""",
            listOf("/api/b" to "GET"), members = """private static final String API = "/api";"""
        )
    }

    fun testServletBlockConsumerRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> { b.GET("/b", h::list); b.POST("/c", h::list); }).build()""",
            listOf("/a/b" to "GET", "/a/c" to "POST")
        )
    }

    fun testReactiveBlockConsumerRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> { b.GET("/b", h::list); b.POST("/c", h::list); }).build()""",
            listOf("/a/b" to "GET", "/a/c" to "POST")
        )
    }

    fun testServletStaticallyImportedPathPredicate() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(path("/a"), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET"), staticPathImport = true
        )
    }

    fun testReactiveStaticallyImportedPathPredicate() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(path("/a"), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET"), staticPathImport = true
        )
    }

    fun testServletVerbWithPredicateOverload() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> b.GET("/b", RequestPredicates.accept(APPLICATION_JSON), h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testReactiveVerbWithPredicateOverload() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> b.GET("/b", RequestPredicates.accept(APPLICATION_JSON), h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testServletMethodReferenceCallbackIsNotTraversed() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", this::routes).build()""", emptyList(),
            members = """
                private Handler h;
                private void routes(RouterFunctions.Builder b) { b.GET("/b", h::list); }
            """.trimIndent()
        )
    }

    fun testReactiveMethodReferenceCallbackIsNotTraversed() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", this::routes).build()""", emptyList(),
            members = """
                private Handler h;
                private void routes(RouterFunctions.Builder b) { b.GET("/b", h::list); }
            """.trimIndent()
        )
    }

    fun testServletConsumerExcludesUnrelatedVerb() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> { other.GET("/z"); b.GET("/b", h::list); }).build()""",
            listOf("/a/b" to "GET"), unrelatedVerb = true,
            members = """
                private final Other other = new Other();
                private static class Other { void GET(String path) {} }
            """.trimIndent()
        )
    }

    fun testReactiveConsumerExcludesUnrelatedVerb() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> { other.GET("/z"); b.GET("/b", h::list); }).build()""",
            listOf("/a/b" to "GET"), unrelatedVerb = true,
            members = """
                private final Other other = new Other();
                private static class Other { void GET(String path) {} }
            """.trimIndent()
        )
    }

    fun testServletBlockSupplierRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", () -> { return RouterFunctions.route().GET("/b", h::list).build(); }).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testReactiveBlockSupplierRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", () -> { return RouterFunctions.route().GET("/b", h::list).build(); }).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testServletComposedPredicateIncludesPathPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path("/a").and(RequestPredicates.accept(APPLICATION_JSON)), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testReactiveComposedPredicateIncludesPathPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path("/a").and(RequestPredicates.accept(APPLICATION_JSON)), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testServletUriLessVerbIsNotListed() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> b.GET(h::list)).build()""", emptyList()
        )
    }

    fun testReactiveUriLessVerbIsNotListed() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> b.GET(h::list)).build()""", emptyList()
        )
    }

    fun testServletPathFreeAndPathPredicateIncludesPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.accept(APPLICATION_JSON).and(RequestPredicates.path("/a")), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testReactivePathFreeAndPathPredicateIncludesPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.accept(APPLICATION_JSON).and(RequestPredicates.path("/a")), b -> b.GET("/b", h::list)).build()""",
            listOf("/a/b" to "GET")
        )
    }

    fun testServletContentTypePredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.contentType(APPLICATION_JSON), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactiveContentTypePredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.contentType(APPLICATION_JSON), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testServletVariablePredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(api, b -> b.GET("/b", h::list)).build()""",
            emptyList(), prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testReactiveVariablePredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(api, b -> b.GET("/b", h::list)).build()""",
            emptyList(), prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testServletHelperPredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(apiPredicate(), b -> b.GET("/b", h::list)).build()""",
            emptyList(),
            members = """private RequestPredicate apiPredicate() { return RequestPredicates.path("/api"); }"""
        )
    }

    fun testReactiveHelperPredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(apiPredicate(), b -> b.GET("/b", h::list)).build()""",
            emptyList(),
            members = """private RequestPredicate apiPredicate() { return RequestPredicates.path("/api"); }"""
        )
    }

    fun testServletOrPredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path("/a").or(RequestPredicates.path("/b")), b -> b.GET("/b", h::list)).build()""",
            emptyList()
        )
    }

    fun testReactiveOrPredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path("/a").or(RequestPredicates.path("/b")), b -> b.GET("/b", h::list)).build()""",
            emptyList()
        )
    }

    fun testServletNegatedPredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path("/a").negate(), b -> b.GET("/b", h::list)).build()""",
            emptyList()
        )
    }

    fun testReactiveNegatedPredicateDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path("/a").negate(), b -> b.GET("/b", h::list)).build()""",
            emptyList()
        )
    }

    fun testServletUndecidableNestPreservesSiblingRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().GET("/x", h::x).nest(api, b -> b.GET("/b", h::list)).POST("/y", h::y).build()""",
            listOf("/x" to "GET", "/y" to "POST"),
            prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testReactiveUndecidableNestPreservesSiblingRoutes() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().GET("/x", h::x).nest(api, b -> b.GET("/b", h::list)).POST("/y", h::y).build()""",
            listOf("/x" to "GET", "/y" to "POST"),
            prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testServletEmptyPathPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path(""), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactiveEmptyPathPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path(""), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testServletAndPathPredicatesComposeInOrder() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path("/a").and(RequestPredicates.path("/b")), b -> b.GET("/c", h::list)).build()""",
            listOf("/a/b/c" to "GET")
        )
    }

    fun testReactiveAndPathPredicatesComposeInOrder() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path("/a").and(RequestPredicates.path("/b")), b -> b.GET("/c", h::list)).build()""",
            listOf("/a/b/c" to "GET")
        )
    }

    fun testServletUnknownAndSideDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path("/a").and(api), b -> b.GET("/b", h::list)).build()""",
            emptyList(), prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testReactiveUnknownAndSideDropsSubtree() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path("/a").and(api), b -> b.GET("/b", h::list)).build()""",
            emptyList(), prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testServletHeadersPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.headers(headers -> true), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactiveHeadersPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.headers(headers -> true), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testServletMethodPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.method(org.springframework.http.HttpMethod.GET), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactiveMethodPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.method(org.springframework.http.HttpMethod.GET), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testServletMethodsPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.methods(org.springframework.http.HttpMethod.GET, org.springframework.http.HttpMethod.POST), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactiveMethodsPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.methods(org.springframework.http.HttpMethod.GET, org.springframework.http.HttpMethod.POST), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testServletAllPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.all(), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactiveAllPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.all(), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testServletParamPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.param("x", value -> true), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testReactiveQueryParamPredicateAddsNoPrefix() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.queryParam("x", value -> true), b -> b.GET("/b", h::list)).build()""",
            listOf("/b" to "GET")
        )
    }

    fun testServletVerbPredicateNestKeepsItsPath() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.PUT("/a"), b -> b.POST("/b", h::list)).build()""",
            listOf("/a/b" to "POST"),
            predicateFactories = setOf("PUT")
        )
    }

    fun testReactiveVerbPredicateNestKeepsItsPath() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.PUT("/a"), b -> b.POST("/b", h::list)).build()""",
            listOf("/a/b" to "POST"),
            predicateFactories = setOf("PUT")
        )
    }

    fun testServletVerbAndPathFreePredicateNestKeepsItsPath() {
        assertBuilderRoutes(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.PUT("/a").and(RequestPredicates.accept(APPLICATION_JSON)), b -> b.POST("/b", h::list)).build()""",
            listOf("/a/b" to "POST"),
            predicateFactories = setOf("PUT", "accept")
        )
    }

    fun testReactiveVerbAndPathFreePredicateNestKeepsItsPath() {
        assertBuilderRoutes(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.PUT("/a").and(RequestPredicates.accept(APPLICATION_JSON)), b -> b.POST("/b", h::list)).build()""",
            listOf("/a/b" to "POST"),
            predicateFactories = setOf("PUT", "accept")
        )
    }

    private fun assertBuilderRoutes(
        type: EndpointType,
        route: String,
        expected: List<Pair<String, String>>,
        members: String = "",
        staticPathImport: Boolean = false,
        unrelatedVerb: Boolean = false,
        prelude: String = "",
        predicateFactories: Set<String> = emptySet()
    ) {
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
            import $functionPackage.RequestPredicate;
            import $functionPackage.RequestPredicates;
            import $functionPackage.RouterFunction;
            import $functionPackage.RouterFunctions;
            import $functionPackage.ServerRequest;
            import $functionPackage.ServerResponse;
            import static org.springframework.http.MediaType.APPLICATION_JSON;
            ${if (staticPathImport) "import static $functionPackage.RequestPredicates.path;" else ""}

            @Configuration
            public class NestedBuilderRouterConfig {
                $members

                @Bean
                public RouterFunction<ServerResponse> routes(Handler h) {
                    $prelude
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
        if (unrelatedVerb) {
            assertEquals(1, builderCalls.count { it.methodExpression.qualifierExpression?.text == "other" })
        }
        builderCalls.forEach { call ->
            val expectedOwner = if (unrelatedVerb && call.methodExpression.qualifierExpression?.text == "other") {
                "NestedBuilderRouterConfig.Other"
            } else {
                "$functionPackage.RouterFunctions.Builder"
            }
            assertEquals(
                "Call must resolve to its expected owner: ${call.text}",
                expectedOwner,
                call.resolveMethod()?.containingClass?.qualifiedName
            )
        }
        val factoryCalls = PsiTreeUtil.findChildrenOfType(config, PsiMethodCallExpression::class.java)
            .filter { it.methodExpression.referenceName in predicateFactories }
        assertEquals(predicateFactories, factoryCalls.mapTo(mutableSetOf()) { it.methodExpression.referenceName })
        factoryCalls.forEach { call ->
            val method = call.resolveMethod()
            assertEquals(call.text, "$functionPackage.RequestPredicates", method?.containingClass?.qualifiedName)
            assertEquals(call.text, "$functionPackage.RequestPredicate", method?.returnType?.canonicalText)
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
