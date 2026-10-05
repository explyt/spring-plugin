/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking

/**
 * How the endpoint tools classify a multipart parameter, by the web stack the application runs on.
 *
 * Servlet MVC binds a multipart part to `@RequestParam` as well as to `@RequestPart`; WebFlux binds `@RequestParam`
 * to the query only. Spring Boot runs a servlet application whenever `DispatcherServlet` is on the classpath, even with
 * WebFlux next to it - there usually for `WebClient` - so only the classpath without it is reactive.
 */
abstract class McpToolsetWebStackTestCase : ExplytJavaLightTestCase() {

    protected val toolset = SpringBootApplicationMcpToolset()
    protected val mapper = ObjectMapper()

    protected fun projectPath(): String = project.basePath ?: ""

    protected fun assertDispatchers(servlet: Boolean, reactive: Boolean) {
        val facade = JavaPsiFacade.getInstance(project)
        val scope = module.moduleWithLibrariesScope
        assertEquals(
            "DispatcherServlet on the classpath", servlet,
            facade.findClass("org.springframework.web.servlet.DispatcherServlet", scope) != null
        )
        assertEquals(
            "DispatcherHandler on the classpath", reactive,
            facade.findClass("org.springframework.web.reactive.DispatcherHandler", scope) != null
        )
    }

    protected fun addUploadController() {
        myFixture.addFileToProject(
            "com/example/app/web/LogoController.kt", """
            package com.example.app.web

            import org.springframework.http.MediaType
            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RequestPart
            import org.springframework.web.bind.annotation.RestController
            import org.springframework.web.multipart.MultipartFile

            @RestController
            class LogoController {
                @PostMapping("/api/stores/{id}/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
                fun uploadLogo(
                    @PathVariable id: Long,
                    @RequestParam("file") file: MultipartFile,
                    @RequestPart("meta") meta: String,
                ): ResponseEntity<Void> = ResponseEntity.ok().build()
            }
            """.trimIndent()
        )
    }

    protected suspend fun sourcesOfUpload(): Map<String, Map<String, String>> {
        val found = mapper.readTree(
            toolset.findEndpoint(urlPattern = "/api/stores/1/logo", projectPath = projectPath())
        )["endpoints"].single()
        val contract = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/api/stores/1/logo", projectPath = projectPath())
        )["endpoints"].single()
        return mapOf("find" to sourcesOf(found), "contract" to sourcesOf(contract))
    }

    protected fun addExchangeController() {
        myFixture.addFileToProject(
            "com/example/app/web/ExchangeController.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController
            import org.springframework.web.server.ServerWebExchange
            import org.springframework.web.server.WebSession

            @RestController
            class ExchangeController {
                @GetMapping("/api/exchange")
                fun exchange(exchange: ServerWebExchange, session: WebSession, name: String): String = name
            }
            """.trimIndent()
        )
    }

    protected fun assertResolving(vararg classNames: String) {
        val facade = JavaPsiFacade.getInstance(project)
        for (className in classNames) {
            assertNotNull("$className on the module classpath", facade.findClass(className, module.moduleWithLibrariesScope))
        }
    }

    protected suspend fun contractSourcesOf(url: String): Map<String, String> =
        sourcesOf(mapper.readTree(toolset.getEndpointContract(urlPattern = url, projectPath = projectPath()))["endpoints"].single())

    protected fun sourcesOf(endpoint: JsonNode): Map<String, String> =
        endpoint["parameters"].associate { it["name"].asText() to it["source"].asText() }
}

internal const val SERVER_WEB_EXCHANGE = "org.springframework.web.server.ServerWebExchange"
internal const val WEB_SESSION = "org.springframework.web.server.WebSession"

/** The reported project carried both starters; the endpoint itself was already typed `Spring MVC`. */
class SpringBootApplicationMcpToolsetMixedStackTest : McpToolsetWebStackTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    fun testMultipartRequestParamIsAPartWhenWebFluxSitsNextToServletMvc() = runBlocking<Unit> {
        assertDispatchers(servlet = true, reactive = true)
        addUploadController()

        for ((tool, sources) in sourcesOfUpload()) {
            assertEquals("$tool sources", mapOf("id" to "PATH", "file" to "PART", "meta" to "PART"), sources)
        }
    }

    fun testReactiveExchangeTypesAreNotFrameworkSuppliedToAServletApplication() = runBlocking<Unit> {
        assertDispatchers(servlet = true, reactive = true)
        assertResolving(SERVER_WEB_EXCHANGE, WEB_SESSION)
        addExchangeController()

        assertEquals(
            mapOf("exchange" to "MODEL", "session" to "MODEL", "name" to "QUERY"),
            contractSourcesOf("/api/exchange"),
        )
    }
}

class SpringBootApplicationMcpToolsetReactiveStackTest : McpToolsetWebStackTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    fun testRequestParamOfAReactiveApplicationStaysAQueryParameter() = runBlocking<Unit> {
        assertDispatchers(servlet = false, reactive = true)
        addUploadController()

        for ((tool, sources) in sourcesOfUpload()) {
            assertEquals("$tool sources", mapOf("id" to "PATH", "file" to "QUERY", "meta" to "PART"), sources)
        }
    }

    fun testReactiveExchangeTypesAreFrameworkSuppliedToAReactiveApplication() = runBlocking<Unit> {
        assertDispatchers(servlet = false, reactive = true)
        assertResolving(SERVER_WEB_EXCHANGE, WEB_SESSION)
        addExchangeController()

        assertEquals(
            mapOf("exchange" to "FRAMEWORK", "session" to "FRAMEWORK", "name" to "QUERY"),
            contractSourcesOf("/api/exchange"),
        )
    }

    fun testSuspendHandlerReportsItsDeclaredParametersWithTheExchangeFrameworkSupplied() = runBlocking<Unit> {
        assertDispatchers(servlet = false, reactive = true)
        assertResolving(SERVER_WEB_EXCHANGE)
        myFixture.addFileToProject(
            "com/example/app/web/SuspendExchangeController.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RestController
            import org.springframework.web.server.ServerWebExchange

            @RestController
            class SuspendExchangeController {
                @GetMapping("/api/suspend-exchange/{code}")
                suspend fun find(@PathVariable code: String, exchange: ServerWebExchange): String = code
            }
            """.trimIndent()
        )
        val compiled = JavaPsiFacade.getInstance(project)
            .findClass("com.example.app.web.SuspendExchangeController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("find", false).single()
        assertEquals(
            "The compiled handler carries the continuation this test is about",
            listOf("code", "exchange", "\$completion"),
            compiled.parameterList.parameters.map { it.name },
        )

        assertEquals(
            mapOf("code" to "PATH", "exchange" to "FRAMEWORK"),
            contractSourcesOf("/api/suspend-exchange/abc"),
        )
    }
}
