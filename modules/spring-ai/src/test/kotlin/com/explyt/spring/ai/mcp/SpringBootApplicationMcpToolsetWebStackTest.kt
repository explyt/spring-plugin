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

    private fun sourcesOf(endpoint: JsonNode): Map<String, String> =
        endpoint["parameters"].associate { it["name"].asText() to it["source"].asText() }
}

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
}
