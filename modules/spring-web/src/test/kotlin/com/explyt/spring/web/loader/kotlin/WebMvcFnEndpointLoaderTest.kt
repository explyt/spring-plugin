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
import com.intellij.psi.JavaPsiFacade

/**
 * WebMvc.fn routes live in `org.springframework.web.servlet.function`, a Spring MVC project that has no Reactor on its
 * classpath at all. The libraries here deliberately exclude WebFlux, so this fixture also proves the discovery no
 * longer depends on `reactor.core.publisher.Flux` being resolvable.
 */
class WebMvcFnEndpointLoaderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springBoot_3_1_1
    )

    fun testReactorIsAbsentSoTheFixtureProvesTheApplicabilityChange() {
        val reactorFlux = JavaPsiFacade.getInstance(module.project)
            .findClass("reactor.core.publisher.Flux", module.moduleWithLibrariesScope)

        assertNull(
            "this fixture must not carry Reactor, otherwise it cannot show that servlet routes no longer need it",
            reactorFlux
        )
    }

    fun testServletRouterDslRouteIsListedAsSpringMvc() {
        addServletRouterConfig(
            routes = """
                GET("/api/users", handler::list)
            """
        )

        assertEquals(listOf("/api/users" to "GET"), servletEndpoints())
    }

    fun testServletNestPrefixAndConstantPathAreResolved() {
        addServletRouterConfig(
            routes = """
                "/api".nest {
                    GET(USERS, handler::list)
                }
            """,
            companionBody = """
                const val USERS = "/users"
            """
        )

        assertEquals(listOf("/api/users" to "GET"), servletEndpoints())
    }

    private fun addServletRouterConfig(routes: String, companionBody: String = "") {
        val companion = if (companionBody.isBlank()) "" else """
            |
            |    companion object {
            |${companionBody.trimIndent().prependIndent("        ")}
            |    }
        """.trimMargin()

        myFixture.addFileToProject(
            "UserHandler.kt",
            """
            import org.springframework.stereotype.Service
            import org.springframework.web.servlet.function.ServerRequest
            import org.springframework.web.servlet.function.ServerResponse

            @Service
            class UserHandler {
                fun list(request: ServerRequest): ServerResponse = ServerResponse.ok().build()
            }
            """.trimIndent()
        )

        myFixture.addFileToProject(
            "ServletRouterConfig.kt",
            """
            |import org.springframework.context.annotation.Bean
            |import org.springframework.context.annotation.Configuration
            |import org.springframework.web.servlet.function.RouterFunction
            |import org.springframework.web.servlet.function.ServerResponse
            |import org.springframework.web.servlet.function.router
            |
            |@Configuration
            |class ServletRouterConfig {
            |
            |    @Bean
            |    fun routes(handler: UserHandler): RouterFunction<ServerResponse> = router {
            |${routes.trimIndent().prependIndent("        ")}
            |    }
            |$companion
            |}
            """.trimMargin()
        )
    }

    /**
     * Reads through the extension point rather than through a directly instantiated loader, so the test also proves
     * the loader is registered and reachable.
     */
    private fun servletEndpoints(): List<Pair<String, String>> = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(module.project).asSequence()
        .filter { it.getType() == EndpointType.SPRING_MVC }
        .filter { it.isApplicable(module) }
        .flatMap { it.searchEndpoints(module) }
        .map(::toPathAndMethod)
        .sortedBy { it.first + it.second }
        .toList()

    private fun toPathAndMethod(endpoint: EndpointElement) = endpoint.path to endpoint.requestMethods.single()
}
