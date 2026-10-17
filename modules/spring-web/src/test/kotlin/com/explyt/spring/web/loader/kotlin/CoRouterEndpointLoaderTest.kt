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
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.route.RouterDslRoutes
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.view.EndpointsTreeData
import com.explyt.spring.web.view.EndpointsViewFilter
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UPrefixExpression
import org.jetbrains.uast.toUElementOfType

private const val REACTIVE_FUNCTION_PACKAGE = "org.springframework.web.reactive.function.server"
private const val CO_ROUTER_DSL = "$REACTIVE_FUNCTION_PACKAGE.CoRouterFunctionDsl"
private const val REACTIVE_ROUTER_DSL = "$REACTIVE_FUNCTION_PACKAGE.RouterFunctionDsl"
private const val REQUEST_PREDICATES = "$REACTIVE_FUNCTION_PACKAGE.RequestPredicates"
private const val REQUEST_PREDICATE = "$REACTIVE_FUNCTION_PACKAGE.RequestPredicate"
private const val MEDIA_TYPE_IMPORT = "import org.springframework.http.MediaType"
private const val PREDICATE_IMPORTS = "import $REACTIVE_FUNCTION_PACKAGE.RequestPredicate\n" +
        "import $REACTIVE_FUNCTION_PACKAGE.RequestPredicates"
private const val METHOD_PREDICATE_IMPORTS = "$PREDICATE_IMPORTS\nimport org.springframework.http.HttpMethod"

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

    fun testStringNestPrefixHelperResolvesLiteralAndConstantValues() {
        addRouterConfig(
            """
            "/a".nest { GET("/b", handler::handle) }
            API.nest { GET("/c", handler::handle) }
            """,
            companionBody = """const val API = "/api""""
        )

        assertEquals(listOf("/a"), nestPrefixes(0))
        assertEquals(listOf("/api"), nestPrefixes(1))
    }

    fun testPathPredicateNestPrefixHelperResolvesReceiverAndValueArgumentForms() {
        addRouterConfig(
            """
            path("/a").nest { GET("/b", handler::handle) }
            nest(path("/c")) { GET("/d", handler::handle) }
            """
        )

        assertEquals(listOf("/a"), nestPrefixes(0))
        assertEquals(listOf("/c"), nestPrefixes(1))
        assertTrue(isNest(0))
        assertTrue(isNest(1))
    }

    fun testPathFreePredicateHasNoPrefixAndComposedOneKeepsItsPath() {
        addRouterConfig(
            """
            accept(MediaType.APPLICATION_JSON).nest { GET("/a", handler::handle) }
            (path("/b") and accept(MediaType.APPLICATION_JSON)).nest { GET("/c", handler::handle) }
            """,
            imports = MEDIA_TYPE_IMPORT
        )
        assertOperatorResolvesTo("and", CO_ROUTER_DSL)

        assertEquals(emptyList<String>(), nestPrefixes(0))
        assertEquals(listOf("/b"), nestPrefixes(1))
    }

    fun testUnresolvedStringNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            someString().nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """,
            companionBody = """fun someString(): String = System.getenv("X")"""
        )

        assertNull(nestPrefixes(0))
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testUnresolvedDslPathNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            val x = readLine() ?: ""
            path(x).nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """
        )
        assertPathPredicateCall(isNestReceiver = true)

        assertNull(nestPrefixes(0))
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testStaticRequestPredicatesPathNestKeepsItsPrefix() {
        addRouterConfig(
            """RequestPredicates.path("/a").nest { GET("/b", handler::handle) }""",
            imports = PREDICATE_IMPORTS
        )
        assertCallsResolveTo("path", REQUEST_PREDICATES)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testReactiveRouterStaticRequestPredicatesPathNestKeepsItsPrefix() {
        myFixture.addFileToProject(
            "ReactiveRouterConfig.kt",
            """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.web.reactive.function.server.RequestPredicates
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.router

            @Configuration
            class ReactiveRouterConfig {
                @Bean
                fun routes(): RouterFunction<ServerResponse> = router {
                    RequestPredicates.path("/a").nest { GET("/b") { ServerResponse.ok().build() } }
                }
            }
            """.trimIndent()
        )
        assertCallsResolveTo("path", REQUEST_PREDICATES, fileName = "ReactiveRouterConfig.kt")
        assertCallsResolveTo(
            "nest", REACTIVE_ROUTER_DSL, returnType = "void", argumentCount = 1, fileName = "ReactiveRouterConfig.kt"
        )
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testComposedDotAndPathPredicateNestKeepsThePathPrefix() {
        addRouterConfig(
            """path("/a").and(accept(MediaType.APPLICATION_JSON)).nest { GET("/b", handler::handle) }""",
            imports = MEDIA_TYPE_IMPORT
        )
        assertPathPredicateCall()
        assertCallsResolveTo("and", REQUEST_PREDICATE)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testPathFreeAndPathPredicateNestTakesThePathFromTheRightSide() {
        addRouterConfig(
            """(accept(MediaType.APPLICATION_JSON) and path("/a")).nest { GET("/b", handler::handle) }""",
            imports = MEDIA_TYPE_IMPORT
        )
        assertPathPredicateCall()
        assertCallsResolveTo("accept", CO_ROUTER_DSL, returnType = null)
        assertOperatorResolvesTo("and", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testStringAndPredicateNestKeepsItsPrefix() {
        addRouterConfig(
            """("/a" and accept(MediaType.APPLICATION_JSON)).nest { GET("/b", handler::handle) }""",
            imports = MEDIA_TYPE_IMPORT
        )
        assertOperatorResolvesTo("and", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testDslVerbPredicateNestKeepsItsPathAndIsNotARoute() {
        addRouterConfig("""GET("/a").nest { GET("/b", handler::handle) }""")
        assertCallsResolveTo("GET", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testStaticVerbPredicateNestKeepsItsPath() {
        addRouterConfig(
            """RequestPredicates.GET("/a").nest { GET("/b", handler::handle) }""",
            imports = PREDICATE_IMPORTS
        )
        assertCallsResolveTo("GET", REQUEST_PREDICATES)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testComposedDslVerbPredicateIsNotARoute() {
        addRouterConfig(
            """(GET("/a") and accept(MediaType.APPLICATION_JSON)).nest { GET("/b", handler::handle) }""",
            imports = MEDIA_TYPE_IMPORT
        )
        assertCallsResolveTo("GET", CO_ROUTER_DSL)
        assertOperatorResolvesTo("and", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testVariablePredicateNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            val api = path("/a")
            api.nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """
        )
        assertPathPredicateCall()
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testHelperPredicateNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            api().nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """,
            companionBody = """fun api(): RequestPredicate = RequestPredicates.path("/a")""",
            imports = PREDICATE_IMPORTS
        )
        assertCallsResolveTo("path", REQUEST_PREDICATES)
        assertCallsResolveTo("api", "GatewayProxyRouterConfig.Companion", argumentCount = 0)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testOrPredicateNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            (path("/a") or path("/c")).nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """
        )
        assertPathPredicateCall()
        assertOperatorResolvesTo("or", CO_ROUTER_DSL)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testNegatedPredicateNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            (!path("/a")).nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """
        )
        assertPathPredicateCall()
        assertOperatorResolvesTo("!", CO_ROUTER_DSL)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testContentTypeNestDoesNotContributeAPathPrefix() {
        addRouterConfig(
            """contentType(MediaType.APPLICATION_JSON).nest { GET("/b", handler::handle) }""",
            imports = MEDIA_TYPE_IMPORT
        )
        assertCallsResolveTo("contentType", CO_ROUTER_DSL, returnType = null)
        assertEquals(listOf("/b" to "GET"), webFluxEndpoints())
    }

    fun testHeadersNestDoesNotContributeAPathPrefix() {
        addRouterConfig("""headers { true }.nest { GET("/b", handler::handle) }""")
        assertCallsResolveTo("headers", CO_ROUTER_DSL)
        assertEquals(listOf("/b" to "GET"), webFluxEndpoints())
    }

    fun testIsNestCallRejectsAGetCall() {
        addRouterConfig("""GET("/b", handler::handle)""")
        val calls = callsInFixture()
        assertTrue(calls.any { it.methodName == "GET" && !SpringWebUtil.isNestCall(it) })
    }

    fun testAcceptNestDoesNotContributeAPathPrefix() {
        addRouterConfig(
            """accept(MediaType.APPLICATION_JSON).nest { GET("/b", handler::handle) }""",
            imports = MEDIA_TYPE_IMPORT
        )
        assertCallsResolveTo("accept", CO_ROUTER_DSL, returnType = null)
        assertEquals(listOf("/b" to "GET"), webFluxEndpoints())
    }

    fun testPathPredicateAndStringNestKeepBothPrefixes() {
        addRouterConfig("""path("/a").nest { "/b".nest { GET("/c", handler::handle) } }""")
        assertPathPredicateCall(isNestReceiver = true)
        assertEquals(listOf("/a/b/c" to "GET"), webFluxEndpoints())
    }

    fun testPathPredicateValueArgumentNestKeepsItsPrefix() {
        addRouterConfig("""nest(path("/a")) { GET("/b", handler::handle) }""")
        assertValueArgumentNestHasNoUastMethodName()
        assertPathPredicateCall()
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testTwoArgumentPathRouteIsListedForAnyMethod() {
        addRouterConfig(
            """
            path("/x", handler::handle)
            GET("/y", handler::handle)
        """
        )
        assertTwoArgumentPathRouteResolvesTo(CO_ROUTER_DSL)
        assertEquals(listOf("/x" to "", "/y" to "GET"), webFluxEndpoints())
    }

    fun testTwoArgumentPathRouteIsOneAllRowKeptByAVerbFilter() {
        addRouterConfig("""path("/x", handler::handle)""")
        val endpoint = webFluxEndpointElements().single()
        assertEquals("/x", endpoint.path)
        assertEquals(emptyList<String>(), endpoint.requestMethods)

        assertEquals(listOf("ALL"), EndpointsTreeData.rowVerbsOf(endpoint))
        val kept = EndpointsViewFilter.apply(
            EndpointsTreeData.rowsOf(endpoint), "", setOf("GET"), emptySet(), EndpointsViewFilter.declaredBasePaths()
        )
        assertEquals(listOf("ALL"), kept.map { it.method })
    }

    fun testReactiveRouterTwoArgumentPathRouteIsListedForAnyMethod() {
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
                    path("/x") { ServerResponse.ok().build() }
                    GET("/y") { ServerResponse.ok().build() }
                }
            }
            """.trimIndent()
        )
        assertTwoArgumentPathRouteResolvesTo(REACTIVE_ROUTER_DSL, "ReactiveRouterConfig.kt")
        assertEquals(listOf("/x" to "", "/y" to "GET"), webFluxEndpoints())
    }

    fun testTwoArgumentPathRouteUnderNestComposesThePrefix() {
        addRouterConfig(""""/a".nest { path("/x", handler::handle) }""")
        assertTwoArgumentPathRouteResolvesTo(CO_ROUTER_DSL)
        assertEquals(listOf("/a/x" to ""), webFluxEndpoints())
    }

    fun testOneArgumentPathPredicateIsNotARoute() {
        addRouterConfig("""path("/a").nest { GET("/b", handler::handle) }""")
        assertCallsResolveTo("path", CO_ROUTER_DSL)
        val pathPredicate = callsInFixture().single { it.methodName == "path" }
        assertEquals(1, pathPredicate.valueArgumentCount)
        assertNull(RouterDslRoutes.requestMethodsOf(pathPredicate))
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testTwoArgumentPathCallOutsideTheDslIsNotARoute() {
        addRouterConfig(
            """
            NonDsl.path("/x", handler::handle)
            GET("/y", handler::handle)
            """,
            imports = "object NonDsl { fun path(pattern: String, handler: Any) {} }"
        )
        val call = nonDslPathCall()
        val method = call.resolve()!!
        assertEquals("NonDsl", method.containingClass?.qualifiedName)
        assertEquals("void", method.returnType?.canonicalText)
        assertEquals(2, call.valueArgumentCount)
        assertEquals(2, method.parameterList.parametersCount)
        assertEquals("java.lang.String", method.parameterList.parameters.first().type.canonicalText)
        assertNull(RouterDslRoutes.requestMethodsOf(call))
        assertEquals(listOf("/y" to "GET"), webFluxEndpoints())
    }

    fun testStringValueArgumentNestKeepsItsPrefix() {
        addRouterConfig("""nest("/a") { GET("/b", handler::handle) }""")
        assertValueArgumentNestHasNoUastMethodName()
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testConstantPathPredicateNestKeepsItsPrefix() {
        addRouterConfig(
            """path(API).nest { GET("/b", handler::handle) }""",
            companionBody = """const val API = "/api""""
        )
        assertPathPredicateCall(isNestReceiver = true)
        assertEquals(listOf("/api/b" to "GET"), webFluxEndpoints())
    }

    fun testComposedPathPredicateKeepsThePathPrefix() {
        addRouterConfig(
            """(path("/a") and accept(MediaType.APPLICATION_JSON)).nest { GET("/b", handler::handle) }""",
            imports = MEDIA_TYPE_IMPORT
        )
        assertPathPredicateCall()
        assertOperatorResolvesTo("and", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testUnrelatedStringPathCallNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            NonDsl.path("/a").nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """,
            imports = "object NonDsl { fun path(value: String): String = value }"
        )
        val method = nonDslPathCall().resolve()!!
        assertFalse(method.containingClass?.qualifiedName in SpringWebClasses.ROUTER_DSL_CLASSES)
        assertEquals("java.lang.String", method.returnType?.canonicalText)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testUnrelatedPredicatePathCallNestDropsItsRoutesAndKeepsSiblings() {
        addRouterConfig(
            """
            NonDsl.path("/a").nest { GET("/b", handler::handle) }
            GET("/s", handler::handle)
            """,
            imports = """
            $PREDICATE_IMPORTS
            object NonDsl { fun path(value: String): RequestPredicate = RequestPredicates.path(value) }
            """.trimIndent()
        )
        val method = nonDslPathCall().resolve()!!
        assertEquals("NonDsl", method.containingClass?.qualifiedName)
        assertEquals(REQUEST_PREDICATE, method.returnType?.canonicalText)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testCoRouterDoublePathPredicateNestKeepsBothPrefixes() {
        addRouterConfig("""path("/a").nest { path("/b").nest { GET("/c", handler::handle) } }""")
        assertPathPredicateCall(isNestReceiver = true)
        assertEquals(listOf("/a/b/c" to "GET"), webFluxEndpoints())
    }

    fun testReactiveRouterDoublePathPredicateNestKeepsBothPrefixes() {
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
                    path("/a").nest { path("/b").nest { GET("/c") { ServerResponse.ok().build() } } }
                }
            }
            """.trimIndent()
        )
        assertPathPredicateCall("ReactiveRouterConfig.kt", "RouterFunctionDsl", isNestReceiver = true)
        assertEquals(listOf("/a/b/c" to "GET"), webFluxEndpoints())
    }

    fun testStringRouteIsListedForAnyMethod() {
        addRouterConfig(
            """
            "/items" { handler.handle(it) }
            GET("/y", handler::handle)
            """
        )
        assertStringRoutesResolveToDslInvoke(CO_ROUTER_DSL)
        assertEquals(emptyList<String>(), RouterDslRoutes.requestMethodsOf(stringRouteCalls().single()))
        assertEquals(listOf("/items" to "", "/y" to "GET"), webFluxEndpoints())
    }

    fun testStringRouteUnderStringNestComposesThePrefix() {
        addRouterConfig(
            """"/api".nest { "/health" { ServerResponse.ok().buildAndAwait() } }""",
            imports = "import org.springframework.web.reactive.function.server.buildAndAwait"
        )
        assertStringRoutesResolveToDslInvoke(CO_ROUTER_DSL)
        assertEquals(listOf("/api/health" to ""), webFluxEndpoints())
    }

    fun testStringRouteIsOneAllRowKeptByAVerbFilter() {
        addRouterConfig(""""/items" { handler.handle(it) }""")
        assertStringRoutesResolveToDslInvoke(CO_ROUTER_DSL)
        val endpoint = webFluxEndpointElements().single()
        assertEquals("/items", endpoint.path)
        assertEquals(emptyList<String>(), endpoint.requestMethods)

        assertEquals(listOf("ALL"), EndpointsTreeData.rowVerbsOf(endpoint))
        val kept = EndpointsViewFilter.apply(
            EndpointsTreeData.rowsOf(endpoint), "", setOf("GET"), emptySet(), EndpointsViewFilter.declaredBasePaths()
        )
        assertEquals(listOf("ALL"), kept.map { it.method })
    }

    fun testReactiveRouterStringRouteIsListedForAnyMethod() {
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
                    "/items" { ServerResponse.ok().build() }
                    GET("/y") { ServerResponse.ok().build() }
                }
            }
            """.trimIndent()
        )
        assertStringRoutesResolveToDslInvoke(REACTIVE_ROUTER_DSL, "ReactiveRouterConfig.kt")
        assertEquals(listOf("/items" to "", "/y" to "GET"), webFluxEndpoints())
    }

    fun testStringNestIsNotAnAnyMethodRoute() {
        addRouterConfig(""""/api".nest { GET("/x", handler::handle) }""")
        assertTrue(stringRouteCalls().isEmpty())
        assertNull(RouterDslRoutes.requestMethodsOf(nestCall(0)))
        assertEquals(listOf("/api/x" to "GET"), webFluxEndpoints())
    }

    fun testUserDefinedStringInvokeIsNotARoute() {
        addRouterConfig(
            """
            "/x"(1)
            GET("/y", handler::handle)
            """,
            imports = "operator fun String.invoke(times: Int) {}"
        )
        val call = stringRouteCalls().single()
        val method = call.resolve()!!
        assertEquals("invoke", method.name)
        assertFalse(method.containingClass?.qualifiedName in SpringWebClasses.ROUTER_DSL_CLASSES)
        assertNull(RouterDslRoutes.requestMethodsOf(call))
        assertEquals(listOf("/y" to "GET"), webFluxEndpoints())
    }

    fun testConstantReceiverRouteIsListedForAnyMethod() {
        addRouterConfig(
            """
            ITEMS { handler.handle(it) }
            GET("/y", handler::handle)
            """,
            imports = """const val ITEMS = "/items""""
        )
        assertNonLiteralReceiverRoutesResolveToDslInvoke(CO_ROUTER_DSL)
        assertEquals(listOf("/items" to "", "/y" to "GET"), webFluxEndpoints())
    }

    fun testConstantReceiverRouteUnderStringNestComposesThePrefix() {
        addRouterConfig(
            """"/api".nest { ITEMS { handler.handle(it) } }""",
            imports = """const val ITEMS = "/items""""
        )
        assertNonLiteralReceiverRoutesResolveToDslInvoke(CO_ROUTER_DSL)
        assertEquals(listOf("/api/items" to ""), webFluxEndpoints())
    }

    private fun assertNonLiteralReceiverRoutesResolveToDslInvoke(owner: String) {
        val calls = callsInFixture().filter { it.methodName == "invoke" }
        assertTrue("Fixture must contain a route on a non-literal String receiver", calls.isNotEmpty())
        calls.forEach { call ->
            assertFalse((call.sourcePsi as KtCallExpression).calleeExpression is KtStringTemplateExpression)
            assertEquals("java.lang.String", call.receiverType?.canonicalText)
            val method = call.resolve()
            assertNotNull("${call.sourcePsi?.text} must resolve", method)
            assertEquals(owner, method!!.containingClass?.qualifiedName)
            assertEquals("invoke", method.name)
            assertEquals("void", method.returnType?.canonicalText)
        }
    }

    private fun stringRouteCalls(fileName: String = "GatewayProxyRouterConfig.kt"): List<UCallExpression> {
        val file = myFixture.psiManager.findFile(myFixture.findFileInTempDir(fileName))!!
        return PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .filter { it.calleeExpression is KtStringTemplateExpression }
            .mapNotNull { it.toUElementOfType<UCallExpression>() }
    }

    private fun assertStringRoutesResolveToDslInvoke(owner: String, fileName: String = "GatewayProxyRouterConfig.kt") {
        val calls = stringRouteCalls(fileName)
        assertTrue("Fixture must contain a \"/path\" { } route", calls.isNotEmpty())
        calls.forEach { call ->
            assertEquals("invoke", call.methodName)
            assertEquals("java.lang.String", call.receiverType?.canonicalText)
            assertEquals(1, call.valueArgumentCount)
            val method = call.resolve()
            assertNotNull("${call.sourcePsi?.text} must resolve", method)
            assertEquals(call.sourcePsi?.text, owner, method!!.containingClass?.qualifiedName)
            assertEquals("invoke", method.name)
            assertEquals("void", method.returnType?.canonicalText)
        }
    }

    fun testStringNestPrefixIsJoinedWithASlashlessInnerPath() {
        addRouterConfig(""""/a".nest { GET("b", handler::handle) }""")
        assertCallsResolveTo("GET", CO_ROUTER_DSL, returnType = null, argumentCount = 2)
        assertEquals(listOf("/a"), nestPrefixes(0))
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testStringNestPrefixWithATrailingSlashIsJoinedWithASlashedInnerPath() {
        addRouterConfig(""""/a/".nest { GET("/b", handler::handle) }""")
        assertCallsResolveTo("GET", CO_ROUTER_DSL, returnType = null, argumentCount = 2)
        assertEquals(listOf("/a/"), nestPrefixes(0))
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testStringNestPrefixWithATrailingSlashIsJoinedWithASlashlessInnerPath() {
        addRouterConfig(""""/a/".nest { GET("b", handler::handle) }""")
        assertCallsResolveTo("GET", CO_ROUTER_DSL, returnType = null, argumentCount = 2)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testPathPredicateNestPrefixIsJoinedWithASlashlessInnerPath() {
        addRouterConfig("""path("/a").nest { GET("b", handler::handle) }""")
        assertPathPredicateCall(isNestReceiver = true)
        assertEquals(listOf("/a/b" to "GET"), webFluxEndpoints())
    }

    fun testStringNestPrefixWithAnEmptyInnerPathServesThePrefix() {
        addRouterConfig(""""/a".nest { GET("", handler::handle) }""")
        assertCallsResolveTo("GET", CO_ROUTER_DSL, returnType = null, argumentCount = 2)
        assertEquals(listOf("/a" to "GET"), webFluxEndpoints())
    }

    fun testPrefixAndPathCombinationsAboveTheCapAreNotListed() {
        addCombinationRouterConfig(prefixCount = 11, pathCount = 10)
        assertEquals(11, nestPrefixes(0)?.size)
        assertEquals(emptyList<Pair<String, String>>(), webFluxEndpoints())
    }

    fun testPrefixAndPathCombinationsAtTheCapAreAllListed() {
        addCombinationRouterConfig(prefixCount = 10, pathCount = 10)
        assertEquals(10, nestPrefixes(0)?.size)
        val expected = (1..10).flatMap { prefix -> (1..10).map { path -> "/p$prefix/r$path" to "GET" } }
            .sortedBy { it.first + it.second }
        assertEquals(expected, webFluxEndpoints())
    }

    fun testVerbNestDropsAnInnerRouteOfAnotherVerb() {
        addRouterConfig(
            """
            PUT("/a").nest { POST("/b", handler::handle) }
            GET("/s", handler::handle)
            """
        )
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testVerbNestKeepsAnInnerRouteOfTheSameVerb() {
        addRouterConfig(
            """
            PUT("/a").nest { PUT("/b", handler::handle) }
            GET("/s", handler::handle)
            """
        )
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "PUT", "/s" to "GET"), webFluxEndpoints())
    }

    fun testTwoArgumentPathRouteUnderAVerbNestServesOnlyTheNestVerb() {
        addRouterConfig("""PUT("/a").nest { path("/b", handler::handle) }""")
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertTwoArgumentPathRouteResolvesTo(CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "PUT"), webFluxEndpoints())
    }

    fun testStringRouteUnderAVerbNestServesOnlyTheNestVerb() {
        addRouterConfig("""PUT("/a").nest { "/b" { handler.handle(it) } }""")
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertStringRoutesResolveToDslInvoke(CO_ROUTER_DSL)
        assertEquals(listOf("/a/b" to "PUT"), webFluxEndpoints())
    }

    fun testPathFreeMethodNestDropsAnInnerRouteOfAnotherVerb() {
        addRouterConfig(
            """
            RequestPredicates.method(HttpMethod.PUT).nest { POST("/b", handler::handle) }
            GET("/s", handler::handle)
            """,
            imports = METHOD_PREDICATE_IMPORTS
        )
        assertCallsResolveTo("method", REQUEST_PREDICATES)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testPathFreeMethodNestKeepsAnInnerRouteOfTheSameVerb() {
        addRouterConfig(
            """RequestPredicates.method(HttpMethod.PUT).nest { PUT("/b", handler::handle) }""",
            imports = METHOD_PREDICATE_IMPORTS
        )
        assertCallsResolveTo("method", REQUEST_PREDICATES)
        assertEquals(listOf("/b" to "PUT"), webFluxEndpoints())
    }

    fun testMethodsAndPathNestKeepsAnInnerRouteOfOneOfItsVerbs() {
        addRouterConfig(
            """
            RequestPredicates.methods(HttpMethod.PUT, HttpMethod.POST).and(RequestPredicates.path("/a"))
                .nest { POST("/b", handler::handle) }
            """,
            imports = METHOD_PREDICATE_IMPORTS
        )
        assertCallsResolveTo("path", REQUEST_PREDICATES)
        assertCallsResolveTo("and", REQUEST_PREDICATE)
        assertEquals(listOf("/a/b" to "POST"), webFluxEndpoints())
    }

    fun testNestedVerbNestsWithDisjointVerbsDropTheInnerRoute() {
        addRouterConfig(
            """
            PUT("/a").nest { POST("/b").nest { GET("/c", handler::handle) } }
            GET("/s", handler::handle)
            """
        )
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertCallsResolveTo("POST", CO_ROUTER_DSL)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testNestedVerbNestsOfTheSameVerbKeepTheInnerRoute() {
        addRouterConfig("""PUT("/a").nest { PUT("/b").nest { PUT("/c", handler::handle) } }""")
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b/c" to "PUT"), webFluxEndpoints())
    }

    fun testPathPredicateNestKeepsAnInnerRouteOfAnyVerb() {
        addRouterConfig("""path("/a").nest { POST("/b", handler::handle) }""")
        assertPathPredicateCall(isNestReceiver = true)
        assertEquals(listOf("/a/b" to "POST"), webFluxEndpoints())
    }

    fun testUnresolvableMethodNestKeepsTheInnerRoute() {
        addRouterConfig(
            """
            val m = HttpMethod.valueOf(readLine() ?: "PUT")
            RequestPredicates.method(m).nest { POST("/b", handler::handle) }
            """,
            imports = METHOD_PREDICATE_IMPORTS
        )
        assertCallsResolveTo("method", REQUEST_PREDICATES)
        assertEquals(listOf("/b" to "POST"), webFluxEndpoints())
    }

    fun testNestOfTwoDisjointVerbPredicatesDropsEveryInnerRoute() {
        addRouterConfig(
            """
            (PUT("/a") and POST("/b")).nest {
                GET("/c", handler::handle)
                POST("/d", handler::handle)
            }
            GET("/s", handler::handle)
            """
        )
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertCallsResolveTo("POST", CO_ROUTER_DSL)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    fun testMethodsNestWithOneUnresolvableVerbKeepsTheInnerRoute() {
        addRouterConfig(
            """
            val m = HttpMethod.valueOf(readLine() ?: "PUT")
            RequestPredicates.methods(HttpMethod.PUT, m).nest { POST("/b", handler::handle) }
            """,
            imports = METHOD_PREDICATE_IMPORTS
        )
        assertCallsResolveTo("methods", REQUEST_PREDICATES, argumentCount = 2)
        assertEquals(listOf("/b" to "POST"), webFluxEndpoints())
    }

    fun testStringNestInsideAVerbNestKeepsTheVerbRestriction() {
        addRouterConfig(
            """
            PUT("/a").nest {
                "/b".nest {
                    POST("/c", handler::handle)
                    PUT("/d", handler::handle)
                }
            }
            """
        )
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertEquals(listOf("/a/b/d" to "PUT"), webFluxEndpoints())
    }

    fun testUndecidableNestInsideAVerbNestDropsTheInnerRoute() {
        addRouterConfig(
            """
            val prefix = readLine() ?: "/b"
            PUT("/a").nest { prefix.nest { PUT("/c", handler::handle) } }
            GET("/s", handler::handle)
            """
        )
        assertCallsResolveTo("PUT", CO_ROUTER_DSL)
        assertEquals(listOf("/s" to "GET"), webFluxEndpoints())
    }

    private fun addCombinationRouterConfig(prefixCount: Int, pathCount: Int) {
        addRouterConfig(
            routes = """
                PREFIXES.forEach { prefix -> prefix.nest { PATHS.forEach { uri -> GET(uri, handler::handle) } } }
            """,
            companionBody = """
                val PREFIXES = listOf(${(1..prefixCount).joinToString { "\"/p$it\"" }})
                val PATHS = listOf(${(1..pathCount).joinToString { "\"/r$it\"" }})
            """
        )
        assertCallsResolveTo("GET", CO_ROUTER_DSL, returnType = null, argumentCount = 2)
    }

    private fun assertValueArgumentNestHasNoUastMethodName(fileName: String = "GatewayProxyRouterConfig.kt") {
        val psiFile = myFixture.psiManager.findFile(myFixture.findFileInTempDir(fileName))!!
        val nest = PsiTreeUtil.findChildrenOfType(psiFile, KtCallExpression::class.java)
            .mapNotNull { it.toUElementOfType<UCallExpression>() }
            .single { (it.sourcePsi as? KtCallExpression)?.calleeExpression?.text == "nest" }
        assertNull(
            "A value-argument nest must carry no resolved UAST method name, or callName() would not be needed; was ${nest.methodName}",
            nest.methodName
        )
        assertEquals("nest", (nest.sourcePsi as KtCallExpression).calleeExpression?.text)
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
        val pathCalls = calls.filter { it.methodName == "path" }
        assertTrue(pathCalls.isNotEmpty())
        pathCalls.forEach { pathCall ->
            val method = pathCall.resolve()!!
            assertEquals(
                "org.springframework.web.reactive.function.server.$dslClass",
                method.containingClass?.qualifiedName
            )
            assertTrue(method.containingClass?.qualifiedName in SpringWebClasses.ROUTER_DSL_CLASSES)
            assertEquals(
                "org.springframework.web.reactive.function.server.RequestPredicate",
                method.returnType?.canonicalText
            )
            assertEquals(1, pathCall.valueArgumentCount)
            if (isNestReceiver) {
                val nest = calls.single {
                    it.methodName == "nest" && (it.receiver as? UCallExpression)?.sourcePsi == pathCall.sourcePsi
                }
                val receiver = nest.receiver as UCallExpression
                assertEquals("path", receiver.methodName)
                assertEquals(method, receiver.resolve())
            }
        }
    }

    private fun assertTwoArgumentPathRouteResolvesTo(owner: String, fileName: String = "GatewayProxyRouterConfig.kt") {
        assertCallsResolveTo("path", owner, returnType = "void", argumentCount = 2, fileName = fileName)
        callsInFixture(fileName)
            .filter { (it.sourcePsi as? KtCallExpression)?.calleeExpression?.text == "path" }
            .forEach { call ->
                val parameters = call.resolve()!!.parameterList.parameters
                assertEquals(call.sourcePsi?.text, 2, parameters.size)
                assertEquals(call.sourcePsi?.text, "java.lang.String", parameters.first().type.canonicalText)
            }
    }

    private fun nonDslPathCall(): UCallExpression = callsInFixture()
        .single { it.methodName == "path" && it.receiver?.asSourceString() == "NonDsl" }

    private fun assertCallsResolveTo(
        name: String,
        owner: String,
        returnType: String? = REQUEST_PREDICATE,
        argumentCount: Int = 1,
        fileName: String = "GatewayProxyRouterConfig.kt"
    ) {
        val calls = callsInFixture(fileName).filter {
            (it.sourcePsi as? KtCallExpression)?.calleeExpression?.text == name && it.valueArgumentCount == argumentCount
        }
        assertTrue("Fixture must contain $name with $argumentCount argument(s)", calls.isNotEmpty())
        calls.forEach { call ->
            val method = call.resolve()
            assertNotNull("${call.sourcePsi?.text} must resolve", method)
            assertEquals(call.sourcePsi?.text, owner, method!!.containingClass?.qualifiedName)
            returnType?.let { assertEquals(call.sourcePsi?.text, it, method.returnType?.canonicalText) }
        }
    }

    private fun assertOperatorResolvesTo(operator: String, owner: String) {
        val file = myFixture.psiManager.findFile(myFixture.findFileInTempDir("GatewayProxyRouterConfig.kt"))!!
        val binary = PsiTreeUtil.findChildrenOfType(file, KtBinaryExpression::class.java)
            .filter { it.operationReference.text == operator }
            .map { it.toUElementOfType<UBinaryExpression>()!!.resolveOperator() }
        val prefix = PsiTreeUtil.findChildrenOfType(file, KtPrefixExpression::class.java)
            .filter { it.operationReference.text == operator }
            .map { it.toUElementOfType<UPrefixExpression>()!!.resolveOperator() }
        val operators = binary + prefix
        assertTrue("Fixture must contain the $operator operator", operators.isNotEmpty())
        operators.forEach { method ->
            assertNotNull("$operator must resolve", method)
            assertEquals(owner, method!!.containingClass?.qualifiedName)
            assertEquals(REQUEST_PREDICATE, method.returnType?.canonicalText)
        }
    }

    private fun nestPrefixes(index: Int): List<String>? = SpringWebUtil.getNestPrefixesOrNullIfUnresolved(nestCall(index))

    private fun isNest(index: Int): Boolean = SpringWebUtil.isNestCall(nestCall(index))

    private fun nestCall(index: Int): UCallExpression = callsInFixture()
        .filter { SpringWebUtil.isNestCall(it) }[index]

    private fun callsInFixture(fileName: String = "GatewayProxyRouterConfig.kt"): List<UCallExpression> {
        val file = myFixture.psiManager.findFile(myFixture.findFileInTempDir(fileName))!!
        return PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
            .mapNotNull { it.toUElementOfType<UCallExpression>() }
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
    private fun webFluxEndpoints(): List<Pair<String, String>> = webFluxEndpointElements().asSequence()
        .map(::toPathAndMethod)
        .sortedBy { it.first + it.second }
        .toList()

    private fun webFluxEndpointElements(): List<EndpointElement> = SpringWebEndpointsLoader.EP_NAME
        .getExtensions(module.project)
        .filter { it.getType() == EndpointType.SPRING_WEBFLUX }
        .filter { it.isApplicable(module) }
        .flatMap { it.searchEndpoints(module) }

    private fun toPathAndMethod(endpoint: EndpointElement) = endpoint.path to endpoint.requestMethods.joinToString()
}
