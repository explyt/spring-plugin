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

    fun testServletVerbPredicateOfAStaticRouteHasNoMarker() {
        assertMarkersFollowModel(
            EndpointType.SPRING_MVC,
            """route(RequestPredicates.GET("/x"), h::list)""",
            emptyList()
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

    private fun configureRouterConfig(type: EndpointType, route: String, prelude: String, members: String) {
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
                    $prelude
                    return RouterFunctions.$route;
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
}
