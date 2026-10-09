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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElementOfType

class CoRouterEndpointLoaderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springBoot_3_1_1,
        TestLibrary.kotlin_1_9_22
    )

    override fun setUp() {
        super.setUp()
        addGatewayHandler()
    }

    fun testLiteralPathIsListed() {
        addRouterConfig(
            routes = """
                GET("/v1/models", handler::handle)
            """
        )

        assertEquals(listOf("/v1/models" to "GET"), webFluxEndpoints())
    }

    fun testConstantPathIsResolved() {
        addRouterConfig(
            routes = """
                GET(MODELS, handler::handle)
            """,
            companionBody = """
                const val MODELS = "/v1/models"
            """
        )

        assertEquals(listOf("/v1/models" to "GET"), webFluxEndpoints())
    }

    fun testEveryPathOfAConstantListIsResolved() {
        addRouterConfig(
            routes = """
                PROXIED_GET_PATHS.forEach { GET(it, handler::handle) }
                PROXIED_POST_PATHS.forEach { POST(it, handler::handle) }
            """,
            companionBody = """
                val PROXIED_GET_PATHS = listOf("/v1/models")

                val PROXIED_POST_PATHS = listOf(
                    "/v1/chat/completions",
                    "/v1/messages"
                )
            """
        )

        assertEquals(
            listOf(
                "/v1/chat/completions" to "POST",
                "/v1/messages" to "POST",
                "/v1/models" to "GET"
            ),
            webFluxEndpoints()
        )
    }

    fun testNestPrefixIsJoinedWithAConstantPath() {
        addRouterConfig(
            routes = """
                "/api".nest {
                    GET(MODELS, handler::handle)
                }
            """,
            companionBody = """
                const val MODELS = "/v1/models"
            """
        )

        assertEquals(listOf("/api/v1/models" to "GET"), webFluxEndpoints())
    }

    /**
     * `accept(APPLICATION_JSON).nest { }` is the reference-doc form: its receiver is a request predicate, not a path,
     * so it contributes no prefix - and its routes must stay in the model rather than vanish with it.
     */
    fun testRoutesInsideAPredicateNestAreListedWithoutAPrefix() {
        addRouterConfig(
            routes = """
                accept(MediaType.APPLICATION_JSON).nest {
                    GET("/person/{id}", handler::handle)
                }
                "/api".nest {
                    accept(MediaType.APPLICATION_JSON).nest {
                        POST("/person", handler::handle)
                    }
                }
            """,
            imports = "import org.springframework.http.MediaType"
        )

        assertEquals(listOf("/api/person" to "POST", "/person/{id}" to "GET"), webFluxEndpoints())
    }

    fun testUnresolvablePathIsNotListedAtAll() {
        myFixture.addFileToProject(
            "UnresolvableRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.coRouter

            @Configuration
            class UnresolvableRouterConfig {

                @Bean
                fun routes(handler: GatewayHandler, basePath: String) = coRouter {
                    GET(basePath, handler::handle)
                }
            }
            """.trimIndent()
        )

        assertEquals(emptyList<Pair<String, String>>(), webFluxEndpoints())
    }

    fun testHeadAndOptionsRoutesAreListed() {
        addRouterConfig(
            routes = """
                GET("/health", handler::handle)
                HEAD("/health", handler::handle)
                OPTIONS("/health", handler::handle)
            """
        )

        assertEquals(
            listOf(
                "/health" to "GET",
                "/health" to "HEAD",
                "/health" to "OPTIONS"
            ),
            webFluxEndpoints()
        )
    }

    fun testGenericMethodRouteTakesItsVerbFromTheArgumentAndItsPathFromNest() {
        addRouterConfig(
            routes = """
                "/api".nest {
                    method(HttpMethod.GET, handler::handle)
                }
            """,
            imports = "import org.springframework.http.HttpMethod"
        )

        assertEquals(listOf("/api" to "GET"), webFluxEndpoints())
    }

    /**
     * `router { }` and `coRouter { }` build the same route model and differ only in whether the handlers suspend, so a
     * route declared through the non-coroutine DSL must be discovered identically.
     */
    fun testNonCoroutineReactiveRouterDslIsListed() {
        myFixture.addFileToProject(
            "ReactiveRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.ServerRequest
            import org.springframework.web.reactive.function.server.router

            @Configuration
            class ReactiveRouterConfig {

                @Bean
                fun reactiveRoutes(): RouterFunction<ServerResponse> = router {
                    GET("/api/users") { _: ServerRequest -> ServerResponse.ok().build() }
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("/api/users" to "GET"), webFluxEndpoints())
    }

    fun testCoRouterPathPredicateNestKeepsItsPrefix() {
        addRouterConfig("""path("/a").nest { GET("/b", handler::handle) }""")
        assertPathPredicateCall(isNestReceiver = true)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testReactiveRouterPathPredicateNestKeepsItsPrefix() {
        myFixture.addFileToProject(
            "ReactiveRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.router

            @Configuration
            class ReactiveRouterConfig {
                @Bean
                fun routes(): RouterFunction<ServerResponse> = router {
                    path("/a").nest { GET("/b") { ServerResponse.ok().build() } }
                }
            }
            """.trimIndent()
        )
        assertPathPredicateCall("ReactiveRouterConfig.kt", "RouterFunctionDsl", isNestReceiver = true)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testStringNestKeepsItsPrefix() {
        addRouterConfig(""""/a".nest { GET("/b", handler::handle) }""")
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testAcceptNestDoesNotContributeAPathPrefix() {
        addRouterConfig(
            """accept(MediaType.APPLICATION_JSON).nest { GET("/b", handler::handle) }""",
            imports = "import org.springframework.http.MediaType"
        )
        assertEquals(listOf("/b" to "GET"), webFluxEndpoints())
    }

    fun testPathPredicateAndStringNestKeepBothPrefixes() {
        addRouterConfig("""path("/a").nest { "/b".nest { GET("/c", handler::handle) } }""")
        assertPathPredicateCall(isNestReceiver = true)
        assertEquals(listOf("/a/b/c" to "GET"), webFluxEndpoints())
    }

    fun testPathPredicateValueArgumentNestKeepsItsPrefix() {
        addRouterConfig("""nest(path("/a")) { GET("/b", handler::handle) }""")
        assertPathPredicateCall()
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testTwoArgumentPathRouteDoesNotPrefixItsSibling() {
        addRouterConfig("""
            path("/x", handler::handle)
            GET("/y", handler::handle)
        """)
        assertEquals(listOf("/y" to "GET"), webFluxEndpoints())
    }

    private fun assertPathPredicateCall(
        fileName: String = "GatewayProxyRouterConfig.kt",
        dslClass: String = "CoRouterFunctionDsl",
        isNestReceiver: Boolean = false
    ) {
        val file = myFixture.findFileInTempDir(fileName)
        val psiFile = myFixture.psiManager.findFile(file)!!
        val calls = PsiTreeUtil.findChildrenOfType(psiFile, KtCallExpression::class.java)
            .mapNotNull { it.toUElementOfType<UCallExpression>() }
        val pathCall = calls.single { it.methodName == "path" }
        val method = pathCall.resolve()!!
        assertEquals("org.springframework.web.reactive.function.server.$dslClass", method.containingClass?.qualifiedName)
        assertEquals("org.springframework.web.reactive.function.server.RequestPredicate", method.returnType?.canonicalText)
        assertEquals(1, pathCall.valueArgumentCount)
        if (isNestReceiver) {
            val nest = calls.single { it.methodName == "nest" && it.receiver is UCallExpression }
            val receiver = nest.receiver as UCallExpression
            assertEquals("path", receiver.methodName)
            assertEquals(method, receiver.resolve())
        }
    }

    private fun addRouterConfig(routes: String, companionBody: String = "", imports: String = "") {
        val companion = if (companionBody.isBlank()) "" else """
            |
            |    companion object {
            |${companionBody.trimIndent().prependIndent("        ")}
            |    }
        """.trimMargin()

        val extraImports = if (imports.isBlank()) "" else "\n|$imports"

        myFixture.addFileToProject(
            "GatewayProxyRouterConfig.kt",
            """
            |import org.springframework.context.annotation.Bean
            |import org.springframework.context.annotation.Configuration
            |import org.springframework.web.reactive.function.server.RouterFunction
            |import org.springframework.web.reactive.function.server.ServerResponse
            |import org.springframework.web.reactive.function.server.coRouter$extraImports
            |
            |@Configuration
            |class GatewayProxyRouterConfig {
            |
            |    @Bean
            |    fun routes(handler: GatewayHandler): RouterFunction<ServerResponse> = coRouter {
            |${routes.trimIndent().prependIndent("        ")}
            |    }
            |$companion
            |}
            """.trimMargin()
        )
    }

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

    /**
     * Reads through the extension point rather than through a directly instantiated loader, so the test also proves
     * the loader is registered and reachable.
     */
    private fun webFluxEndpoints(): List<Pair<String, String>> = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(module.project).asSequence()
        .filter { it.getType() == EndpointType.SPRING_WEBFLUX }
        .filter { it.isApplicable(module) }
        .flatMap { it.searchEndpoints(module) }
        .map(::toPathAndMethod)
        .sortedBy { it.first + it.second }
        .toList()

    private fun toPathAndMethod(endpoint: EndpointElement) = endpoint.path to endpoint.requestMethods.single()
}
