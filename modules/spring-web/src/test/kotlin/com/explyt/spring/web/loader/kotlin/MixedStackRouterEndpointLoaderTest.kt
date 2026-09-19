/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.loader.SpringWebFluxEndpointsLoader
import com.explyt.spring.web.loader.SpringWebMvcFnEndpointsLoader

private const val REACTIVE_ROUTE = "/api/reactive"
private const val SERVLET_ROUTE = "/api/servlet"

/**
 * Both stacks on one classpath is the only fixture that can tell the loaders apart: they share an
 * [com.explyt.spring.web.loader.EndpointHandlerChain], so without each one keeping only the routes of its own stack
 * every route would be reported twice — once per loader — and appear twice in the tool window.
 */
class MixedStackRouterEndpointLoaderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1
    )

    override fun setUp() {
        super.setUp()
        addReactiveRouter()
        addServletRouter()
    }

    fun testReactiveRoutesAreListedWithoutTheServletOnes() {
        assertBothLoadersAreApplicable()

        assertEquals(
            listOf(REACTIVE_ROUTE to "GET"),
            endpointsOfType(EndpointType.SPRING_WEBFLUX)
        )
    }

    fun testServletRoutesAreListedWithoutTheReactiveOnes() {
        assertBothLoadersAreApplicable()

        assertEquals(
            listOf(SERVLET_ROUTE to "GET"),
            endpointsOfType(EndpointType.SPRING_MVC)
        )
    }

    fun testEachRouteIsReportedExactlyOnceAcrossAllLoaders() {
        assertBothLoadersAreApplicable()

        val everyEndpoint = allEndpoints()

        assertEquals(
            "$REACTIVE_ROUTE must be reported by one loader only, or it is listed twice in the tool window",
            listOf(REACTIVE_ROUTE to "GET"),
            everyEndpoint.filter { it.first == REACTIVE_ROUTE }
        )
        assertEquals(
            "$SERVLET_ROUTE must be reported by one loader only, or it is listed twice in the tool window",
            listOf(SERVLET_ROUTE to "GET"),
            everyEndpoint.filter { it.first == SERVLET_ROUTE }
        )
    }

    /**
     * Without both loaders actually running, every assertion above holds for the wrong reason — an empty side cannot
     * contain the other stack's routes and cannot duplicate anything.
     */
    private fun assertBothLoadersAreApplicable() {
        assertTrue(
            "the WebFlux loader must run here, otherwise its absence — not the type filter — is what keeps the " +
                    "servlet route out of the WebFlux list",
            loaderOf<SpringWebFluxEndpointsLoader>().isApplicable(module)
        )
        assertTrue(
            "the WebMvc.fn loader must run here, otherwise the servlet route is missing rather than correctly typed",
            loaderOf<SpringWebMvcFnEndpointsLoader>().isApplicable(module)
        )
    }

    private fun addReactiveRouter() {
        myFixture.addFileToProject(
            "ReactiveUserHandler.kt",
            """
            import org.springframework.stereotype.Service
            import org.springframework.web.reactive.function.server.ServerRequest
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.buildAndAwait

            @Service
            class ReactiveUserHandler {
                suspend fun list(request: ServerRequest): ServerResponse = ServerResponse.ok().buildAndAwait()
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "ReactiveRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class ReactiveRouterConfig {

                @Bean
                fun reactiveRoutes(handler: ReactiveUserHandler): RouterFunction<ServerResponse> = coRouter {
                    GET("$REACTIVE_ROUTE", handler::list)
                }
            }
            """.trimIndent()
        )
    }

    /**
     * The servlet stack declares its own `ServerResponse`, so it cannot share a file with the reactive one.
     */
    private fun addServletRouter() {
        myFixture.addFileToProject(
            "ServletUserHandler.kt",
            """
            import org.springframework.stereotype.Service
            import org.springframework.web.servlet.function.ServerRequest
            import org.springframework.web.servlet.function.ServerResponse

            @Service
            class ServletUserHandler {
                fun list(request: ServerRequest): ServerResponse = ServerResponse.ok().build()
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "ServletRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.servlet.function.RouterFunction
            import org.springframework.web.servlet.function.ServerResponse
            import org.springframework.web.servlet.function.router

            @Configuration
            class ServletRouterConfig {

                @Bean
                fun servletRoutes(handler: ServletUserHandler): RouterFunction<ServerResponse> = router {
                    GET("$SERVLET_ROUTE", handler::list)
                }
            }
            """.trimIndent()
        )
    }

    private inline fun <reified T : SpringWebEndpointsLoader> loaderOf(): T = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(module.project)
        .filterIsInstance<T>()
        .single()

    /**
     * Reads through the extension point rather than through a directly instantiated loader, so the test also proves
     * the loader is registered and reachable.
     */
    private fun endpointsOfType(type: EndpointType): List<Pair<String, String>> = endpoints { it.getType() == type }

    private fun allEndpoints(): List<Pair<String, String>> = endpoints { true }

    private fun endpoints(loaderFilter: (SpringWebEndpointsLoader) -> Boolean): List<Pair<String, String>> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter(loaderFilter)
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .map(::toPathAndMethod)
            .sortedBy { it.first + it.second }
            .toList()

    private fun toPathAndMethod(endpoint: EndpointElement) = endpoint.path to endpoint.requestMethods.single()
}
