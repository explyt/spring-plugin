/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.util.WebApplicationStack
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import kotlinx.coroutines.runBlocking

class SpringBootApplicationMcpToolsetWebFluxTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    private fun projectPath(): String = project.basePath ?: ""

    fun testArgumentsSuppliedByTheReactiveFrameworkAreNotModelAttributes() = runBlocking<Unit> {
        assertReactiveStackResolving(
            SERVER_WEB_EXCHANGE, SERVER_WEB_EXCHANGE_DECORATOR, REACTIVE_SERVER_HTTP_REQUEST,
            REACTIVE_SERVER_HTTP_RESPONSE, WEB_SESSION, URI_BUILDER, URI_COMPONENTS_BUILDER,
        )
        myFixture.addFileToProject("com/example/app/web/TracedExchange.java", """
            package com.example.app.web;

            import org.springframework.web.server.ServerWebExchange;
            import org.springframework.web.server.ServerWebExchangeDecorator;

            public class TracedExchange extends ServerWebExchangeDecorator {
                public TracedExchange(ServerWebExchange delegate) { super(delegate); }
            }
        """.trimIndent())
        myFixture.addFileToProject("com/example/app/web/RelativeUriBuilder.java", """
            package com.example.app.web;

            import org.springframework.web.util.UriBuilder;

            public abstract class RelativeUriBuilder implements UriBuilder {
            }
        """.trimIndent())
        myFixture.addFileToProject("com/example/app/web/ExchangeController.java", """
            package com.example.app.web;

            import org.springframework.http.server.reactive.ServerHttpRequest;
            import org.springframework.http.server.reactive.ServerHttpResponse;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.server.ServerWebExchange;
            import org.springframework.web.server.WebSession;
            import org.springframework.web.util.UriBuilder;
            import org.springframework.web.util.UriComponentsBuilder;

            @RestController
            public class ExchangeController {
                @GetMapping("/api/exchange")
                public String exchange(ServerWebExchange exchange, ServerHttpRequest request, ServerHttpResponse response,
                                       WebSession session, UriBuilder uriBuilder, UriComponentsBuilder componentsBuilder,
                                       RelativeUriBuilder relativeUriBuilder, TracedExchange traced) {
                    return "";
                }
            }
        """.trimIndent())

        assertEquals(
            mapOf(
                "exchange" to "FRAMEWORK",
                "request" to "FRAMEWORK",
                "response" to "FRAMEWORK",
                "session" to "FRAMEWORK",
                "uriBuilder" to "FRAMEWORK",
                "componentsBuilder" to "FRAMEWORK",
                "relativeUriBuilder" to "MODEL",
                "traced" to "FRAMEWORK",
            ),
            sourcesOf(contractParameters("/api/exchange")),
        )
    }

    fun testReactiveWrappersOfSessionPrincipalAndErrorsAreFrameworkSupplied() = runBlocking<Unit> {
        assertReactiveStackResolving(WEB_SESSION, MONO, FLUX, COMPLETABLE_FUTURE, "org.reactivestreams.Publisher")
        myFixture.addFileToProject("com/example/app/web/Filter.java", """
            package com.example.app.web;

            public class Filter {
                public String name;
            }
        """.trimIndent())
        myFixture.addFileToProject("com/example/app/web/SessionController.java", """
            package com.example.app.web;

            import java.security.Principal;
            import java.util.concurrent.CompletableFuture;
            import org.springframework.validation.Errors;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.server.WebSession;
            import reactor.core.publisher.Flux;
            import reactor.core.publisher.Mono;

            @RestController
            public class SessionController {
                @GetMapping("/api/session")
                public Mono<String> session(Mono<WebSession> session, Flux<Principal> principals, Mono<Errors> errors,
                                            CompletableFuture<WebSession> futureSession,
                                            Mono<Filter> filtered, Mono rawMono, Mono<? extends WebSession> anySession,
                                            Filter filter, String name) {
                    return Mono.empty();
                }
            }
        """.trimIndent())

        assertEquals(
            mapOf(
                "session" to "FRAMEWORK",
                "principals" to "FRAMEWORK",
                "errors" to "FRAMEWORK",
                "futureSession" to "FRAMEWORK",
                "filtered" to "MODEL",
                "rawMono" to "MODEL",
                "anySession" to "MODEL",
                "filter" to "MODEL",
                "name" to "QUERY",
            ),
            sourcesOf(contractParameters("/api/session")),
        )
    }

    fun testLocaleAndHttpMethodStayFrameworkSuppliedUnlessBoundToTheQuery() = runBlocking<Unit> {
        assertReactiveStackResolving("org.springframework.http.HttpMethod")
        myFixture.addFileToProject("com/example/app/web/LocaleController.java", """
            package com.example.app.web;

            import java.util.Locale;
            import org.springframework.http.HttpMethod;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class LocaleController {
                @GetMapping("/api/locale")
                public String locale(Locale locale, HttpMethod method, @RequestParam Locale requested) {
                    return "";
                }
            }
        """.trimIndent())

        assertEquals(
            mapOf("locale" to "FRAMEWORK", "method" to "FRAMEWORK", "requested" to "QUERY"),
            sourcesOf(contractParameters("/api/locale")),
        )
    }

    private fun assertReactiveStackResolving(vararg classNames: String) {
        assertEquals(WebApplicationStack.REACTIVE, WebApplicationStack.of(module))
        val facade = JavaPsiFacade.getInstance(project)
        for (className in classNames) {
            assertNotNull("$className on the module classpath", facade.findClass(className, module.moduleWithLibrariesScope))
        }
    }

    private suspend fun contractParameters(url: String): JsonNode =
        mapper.readTree(toolset.getEndpointContract(urlPattern = url, projectPath = projectPath()))["endpoints"]
            .single()["parameters"]

    private fun sourcesOf(parameters: JsonNode): Map<String, String> =
        parameters.associate { it["name"].asText() to it["source"].asText() }

    fun testRequestParamMultipartFileRemainsQueryOnWebFlux() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/ReactiveController.java", """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestPart;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.multipart.MultipartFile;

            @RestController
            public class ReactiveController {
                @PostMapping("/api/reactive/upload")
                public String upload(
                        @RequestParam("file") MultipartFile file,
                        @RequestPart("metadata") String metadata) {
                    return file.getName() + metadata;
                }
            }
        """.trimIndent())

        val result = mapper.readTree(toolset.getEndpointContract(
            urlPattern = "/api/reactive/upload",
            projectPath = projectPath(),
        ))
        val parameters = result["endpoints"].single()["parameters"]
        val byName = parameters.associateBy { it["name"].asText() }

        assertEquals("QUERY", byName.getValue("file")["source"].asText())
        assertEquals("PART", byName.getValue("metadata")["source"].asText())
    }

    /** A reactive publisher is a container: the client receives its payload, as from `ResponseEntity`. */
    fun testPublishersAreDescribedByTheirPayload() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/Shipment.java", """
            package com.example.app.web;

            public class Shipment {
                public long id;
            }
        """.trimIndent())
        myFixture.addFileToProject("com/example/app/web/ShipmentController.java", """
            package com.example.app.web;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            import reactor.core.publisher.Flux;
            import reactor.core.publisher.Mono;

            @RestController
            public class ShipmentController {
                @GetMapping("/api/shipments/one")
                public Mono<ResponseEntity<Shipment>> one() { return Mono.empty(); }

                @GetMapping("/api/shipments")
                public Flux<Shipment> all() { return Flux.empty(); }
            }
        """.trimIndent())

        for (url in listOf("/api/shipments/one", "/api/shipments")) {
            val schema = mapper.readTree(toolset.getEndpointContract(urlPattern = url, projectPath = projectPath()))["endpoints"]
                .single { it["fullPath"].asText() == url }["responseSchema"]
            assertEquals("$url is described by its payload", "com.example.app.web.Shipment", schema["className"].asText())
            assertEquals(listOf("id"), schema["fields"].map { it["name"].asText() })
        }
    }

    private companion object {
        const val SERVER_WEB_EXCHANGE_DECORATOR = "org.springframework.web.server.ServerWebExchangeDecorator"
        const val REACTIVE_SERVER_HTTP_REQUEST = "org.springframework.http.server.reactive.ServerHttpRequest"
        const val REACTIVE_SERVER_HTTP_RESPONSE = "org.springframework.http.server.reactive.ServerHttpResponse"
        const val URI_BUILDER = "org.springframework.web.util.UriBuilder"
        const val URI_COMPONENTS_BUILDER = "org.springframework.web.util.UriComponentsBuilder"
        const val MONO = "reactor.core.publisher.Mono"
        const val FLUX = "reactor.core.publisher.Flux"
        const val COMPLETABLE_FUTURE = "java.util.concurrent.CompletableFuture"
    }
}
