/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.parentOfType

class RouterBuilderEndpointLineMarkerProviderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1
    )

    fun testServletPathConsumerMarkerNamesComposedPath() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> b.GET("/b", h::list)).build()""",
            listOf("GET /a/b")
        )
    }

    fun testReactivePathConsumerMarkerNamesComposedPath() {
        assertMarkersFollowModel(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> b.GET("/b", h::list)).build()""",
            listOf("GET /a/b")
        )
    }

    fun testServletNestPathPredicateMarkerNamesComposedPath() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.path("/a"), b -> b.GET("/b", h::list)).build()""",
            listOf("GET /a/b")
        )
    }

    fun testReactiveNestPathPredicateMarkerNamesComposedPath() {
        assertMarkersFollowModel(
            EndpointType.SPRING_WEBFLUX,
            """route().nest(RequestPredicates.path("/a"), b -> b.GET("/b", h::list)).build()""",
            listOf("GET /a/b")
        )
    }

    fun testServletPathSupplierMarkerNamesComposedPath() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().path("/a", () -> RouterFunctions.route().GET("/b", h::list).build()).build()""",
            listOf("GET /a/b")
        )
    }

    fun testReactivePathSupplierMarkerNamesComposedPath() {
        assertMarkersFollowModel(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", () -> RouterFunctions.route().GET("/b", h::list).build()).build()""",
            listOf("GET /a/b")
        )
    }

    fun testServletNestedPathConsumersMarkerNamesAllPrefixes() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().path("/a", b -> b.path("/b", c -> c.GET("/c", h::list))).build()""",
            listOf("GET /a/b/c")
        )
    }

    fun testReactiveNestedPathConsumersMarkerNamesAllPrefixes() {
        assertMarkersFollowModel(
            EndpointType.SPRING_WEBFLUX,
            """route().path("/a", b -> b.path("/b", c -> c.GET("/c", h::list))).build()""",
            listOf("GET /a/b/c")
        )
    }

    fun testServletVerbPredicateArgumentOfARouteHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().GET("/x", RequestPredicates.GET("/z"), h::list).build()""",
            listOf("GET /x")
        )
    }

    fun testServletVerbPredicateOfANestHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.PUT("/a"), b -> b.PUT("/b", h::list)).build()""",
            listOf("PUT /a/b")
        )
    }

    fun testServletStaticRoutePredicateKeepsItsMarker() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_MVC,
            """return RouterFunctions.route(RequestPredicates.GET("/x"), h::list);""",
            listOf("GET /x @RequestPredicates")
        )
    }

    fun testIdentifierInsideAnAnonymousHandlerGetsNoExtraMarker() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_MVC,
            ANONYMOUS_SERVLET_HANDLER,
            listOf("GET /x @RequestPredicates")
        )
    }

    fun testReactiveIdentifierInsideAnAnonymousHandlerGetsNoExtraMarker() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_WEBFLUX,
            ANONYMOUS_REACTIVE_HANDLER,
            listOf("GET /x @RequestPredicates")
        )
    }

    fun testServletStaticAndRouteChainKeepsItsMarkers() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_MVC,
            STATIC_AND_ROUTE_CHAIN,
            listOf("GET /x @RequestPredicates", "POST /y @RequestPredicates")
        )
    }

    fun testReactiveStaticAndRouteChainKeepsItsMarkers() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_WEBFLUX,
            STATIC_AND_ROUTE_CHAIN,
            listOf("GET /x @RequestPredicates", "POST /y @RequestPredicates")
        )
    }

    fun testServletLocalVariableBuilderStatementsKeepTheirMarkers() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_MVC,
            LOCAL_VARIABLE_BUILDER,
            listOf("GET /x @Builder", "POST /y @Builder")
        )
    }

    fun testReactiveLocalVariableBuilderStatementsKeepTheirMarkers() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_WEBFLUX,
            LOCAL_VARIABLE_BUILDER,
            listOf("GET /x @Builder", "POST /y @Builder")
        )
    }

    fun testServletIfElseBranchChainsKeepTheirMarkers() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_MVC,
            IF_ELSE_BRANCH_CHAINS,
            listOf("GET /x @Builder", "POST /y @Builder"),
            members = FLAG_FIELD
        )
    }

    fun testReactiveIfElseBranchChainsKeepTheirMarkers() {
        assertMarkersOutsideTheModel(
            EndpointType.SPRING_WEBFLUX,
            IF_ELSE_BRANCH_CHAINS,
            listOf("GET /x @Builder", "POST /y @Builder"),
            members = FLAG_FIELD
        )
    }

    fun testServletTopLevelRouteMarkerControl() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().GET("/x", h::list).build()""",
            listOf("GET /x")
        )
    }

    fun testReactiveTopLevelRouteMarkerControl() {
        assertMarkersFollowModel(
            EndpointType.SPRING_WEBFLUX,
            """route().GET("/x", h::list).build()""",
            listOf("GET /x")
        )
    }

    fun testServletRouteUnderUndecidableNestHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().nest(api, b -> b.GET("/b", h::list)).build()""",
            emptyList(),
            prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testServletUndecidableNestKeepsSiblingMarkers() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().GET("/x", h::list).nest(api, b -> b.GET("/b", h::list)).POST("/y", h::list).build()""",
            listOf("GET /x", "POST /y"),
            prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testServletRouteUnderUnresolvedPathPrefixHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().path(prefix, b -> b.GET("/b", h::list)).build()""",
            emptyList(),
            members = """private final String prefix = System.getProperty("prefix");"""
        )
    }

    fun testServletMixedRoutesMarkersMatchTheModel() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route()
                .GET("/x", h::list)
                .path("/a", b -> b
                    .GET("/b", RequestPredicates.accept(APPLICATION_JSON), h::list)
                    .nest(RequestPredicates.path("/c"), c -> c.POST("/d", h::list)))
                .nest(api, b -> b.DELETE("/e", h::list))
                .build()""",
            listOf("GET /a/b", "GET /x", "POST /a/c/d"),
            prelude = """RequestPredicate api = RequestPredicates.path("/api");"""
        )
    }

    fun testServletRouteOfAnotherVerbUnderAVerbNestHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            VERB_NEST_WITH_TWO_VERBS,
            listOf("GET /s", "PUT /a/c")
        )
    }

    fun testReactiveRouteOfAnotherVerbUnderAVerbNestHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_WEBFLUX,
            VERB_NEST_WITH_TWO_VERBS,
            listOf("GET /s", "PUT /a/c")
        )
    }

    fun testServletRouteOfAnotherVerbUnderAPathFreeMethodNestHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route().nest(RequestPredicates.method(org.springframework.http.HttpMethod.PUT), b -> b.POST("/b", h::list).PUT("/c", h::list)).build()""",
            listOf("PUT /c")
        )
    }

    private fun assertMarkersFollowModel(
        type: EndpointType,
        route: String,
        expected: List<String>,
        prelude: String = "",
        members: String = ""
    ) {
        configureRouterConfig(type, route, prelude, members)

        assertEquals("Precondition: the endpoint model lists the composed routes", expected, modelRoutes(type))

        myFixture.doHighlighting()
        assertEquals(expected.map { "$it @Builder" }, gutterRoutes())
    }

    private fun assertMarkersOutsideTheModel(
        type: EndpointType,
        body: String,
        expected: List<String>,
        members: String = ""
    ) {
        configureRouterConfig(type, route = "", prelude = "", members = members, body = body)

        assertEquals("Precondition: the endpoint model does not list this form (#627)", emptyList<String>(), modelRoutes(type))

        myFixture.doHighlighting()
        assertEquals(expected, gutterRoutes())
    }

    private fun configureRouterConfig(
        type: EndpointType,
        route: String,
        prelude: String,
        members: String,
        body: String = "$prelude return RouterFunctions.$route;"
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
        myFixture.configureByText(
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

            @Configuration
            public class NestedBuilderRouterConfig {
                $members

                @Bean
                public RouterFunction<ServerResponse> routes(Handler h) {
                    $body
                }

                public interface Handler {
                    $responseType list(ServerRequest request);
                }
            }
            """.trimIndent()
        )
    }

    private fun modelRoutes(type: EndpointType): List<String> = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(project).asSequence()
        .filter { it.getType() == type }
        .filter { it.isApplicable(module) }
        .flatMap { it.searchEndpoints(module) }
        .map { "${it.requestMethods.joinToString(",")} ${it.path}" }
        .sorted()
        .toList()

    private fun gutterRoutes(): List<String> = myFixture.findAllGutters()
        .filter { it.icon == SpringIcons.ReadAccess }
        .filter { it.tooltipText == "Endpoint Actions" }
        .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
        .map { renderer ->
            val endpointInfo = (renderer.lineMarkerInfo.navigationHandler as EndpointIconGutterHandler).endpointInfo
            val anchorOwner = renderer.lineMarkerInfo.element
                ?.parentOfType<PsiMethodCallExpression>()
                ?.resolveMethod()?.containingClass?.name
            "${endpointInfo.requestMethods.joinToString(",")} ${endpointInfo.path} @$anchorOwner"
        }
        .sorted()

    private companion object {
        const val VERB_NEST_WITH_TWO_VERBS =
            """route().GET("/s", h::list).nest(RequestPredicates.PUT("/a"), b -> b.POST("/b", h::list).PUT("/c", h::list)).build()"""

        const val ANONYMOUS_SERVLET_HANDLER =
            """return RouterFunctions.route(RequestPredicates.GET("/x"), new HandlerFunction<ServerResponse>() {
                public ServerResponse handle(ServerRequest method) { return ServerResponse.ok().build(); }
            });"""

        const val ANONYMOUS_REACTIVE_HANDLER =
            """return RouterFunctions.route(RequestPredicates.GET("/x"), new HandlerFunction<ServerResponse>() {
                public reactor.core.publisher.Mono<ServerResponse> handle(ServerRequest method) {
                    return reactor.core.publisher.Mono.empty();
                }
            });"""

        const val STATIC_AND_ROUTE_CHAIN =
            """return RouterFunctions.route(RequestPredicates.GET("/x"), h::list).andRoute(RequestPredicates.POST("/y"), h::list);"""

        const val LOCAL_VARIABLE_BUILDER =
            """RouterFunctions.Builder b = RouterFunctions.route(); b.GET("/x", h::list); b.POST("/y", h::list); return b.build();"""

        const val IF_ELSE_BRANCH_CHAINS =
            """if (flag) { return RouterFunctions.route().GET("/x", h::list).build(); } else { return RouterFunctions.route().POST("/y", h::list).build(); }"""

        const val FLAG_FIELD = """private final boolean flag = Boolean.getBoolean("flag");"""
    }
}
