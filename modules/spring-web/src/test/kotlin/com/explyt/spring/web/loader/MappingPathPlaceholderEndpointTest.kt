/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.spring.core.properties.FoldedPropertyValue
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.providers.ControllerEndpointActionsLineMarkerProvider
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.explyt.spring.web.providers.EndpointRunLineMarkerProvider
import com.explyt.spring.web.providers.RunInSwaggerAction
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * A mapping path holding a property placeholder is registered by Spring with the configured value, so the endpoint
 * model - the Endpoints tool window, URL references, the MCP tools, the gutter and the HTTP client - has to show and
 * match that value rather than the `${...}` text, which no request URL ever contains.
 */
class MappingPathPlaceholderEndpointTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
    )

    fun testPathIsResolvedFromYaml() {
        addConfiguration("application.yml", "app:\n  shortener-path: /l\n")
        addController()

        val endpoint = redirectEndpoint()
        assertEquals("/l/{code}", endpoint.path)
        assertEquals("The declared template stays available", "/\${app.shortener-path:/d}/{code}", endpoint.pathTemplate)
    }

    fun testPathIsResolvedFromProperties() {
        addConfiguration("application.properties", "app.shortener-path=/p\n")
        addController()

        assertEquals("/p/{code}", redirectEndpoint().path)
    }

    fun testDefaultIsUsedWhenTheKeyIsUndefined() {
        addController()

        assertEquals("/d/{code}", redirectEndpoint().path)
    }

    fun testUnresolvablePlaceholderIsKeptAsWritten() {
        addController(prefix = "\${app.shortener-path}")

        val endpoint = redirectEndpoint()
        assertEquals("/\${app.shortener-path}/{code}", endpoint.path)
        assertEquals(endpoint.path, endpoint.pathTemplate)
    }

    fun testConcreteUrlFindsTheEndpoint() {
        addConfiguration("application.yml", "app:\n  shortener-path: /l\n")
        addController()

        val found = SpringWebEndpointsSearcher.getInstance(project).getAllEndpointElements("/l/offer", module)
        assertEquals(listOf("/l/{code}"), found.map { it.path })
    }

    fun testPathFollowsAnEditedConfiguration() {
        val configuration = addConfiguration("application.yml", "app:\n  shortener-path: /l\n")
        addController()
        assertEquals("/l/{code}", redirectEndpoint().path)

        val document = PsiDocumentManager.getInstance(project).getDocument(configuration)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText("app:\n  shortener-path: /m\n") }
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals("/m", FoldedPropertyValue.resolve(module, "app.shortener-path")?.value)
        assertEquals("/m/{code}", redirectEndpoint().path)
    }

    fun testEndpointInfoOfTheGutterAndTheHttpClientIsResolved() {
        addConfiguration("application.yml", "app:\n  shortener-path: /l\n")
        addController()
        val method = JavaPsiFacade.getInstance(project)
            .findClass("com.example.RedirectController", GlobalSearchScope.allScope(project))!!
            .findMethodsByName("redirect", false).single().toUElement() as UMethod

        val info = SpringWebUtil.getEndpointInfo(method, "\${app.shortener-path:/d}")

        assertEquals("/l/{code}", info?.path)
    }

    fun testKotlinControllerPathIsResolved() {
        addConfiguration("application.yml", "app:\n  shortener-path: /l\n")
        myFixture.addFileToProject(
            "com/example/KotlinRedirectController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("\${'$'}{app.shortener-path:/d}")
            class KotlinRedirectController {
                @GetMapping("/{code}")
                fun redirect(@PathVariable code: String): String = code
            }
            """.trimIndent()
        )

        assertEquals("/l/{code}", redirectEndpoint().path)
    }

    /**
     * "Run in Swagger" builds its request from the mapping on its own rather than from the endpoint model, so it has
     * to resolve the placeholder too, or the request goes to a path the application never registered.
     */
    fun testRunInSwaggerMarkerRequestsTheResolvedPath() {
        addConfiguration("application.yml", "app:\n  shortener-path: /l\n")
        addController()
        val method = JavaPsiFacade.getInstance(project)
            .findClass("com.example.RedirectController", GlobalSearchScope.allScope(project))!!
            .findMethodsByName("redirect", false).single()

        val info = EndpointRunLineMarkerProvider().getInfo(method.nameIdentifier!!)
        val action = info?.actions?.filterIsInstance<RunInSwaggerAction>()?.single()
            ?: error("Expected a Run in Swagger marker on the handler")

        assertEquals(listOf("/l/{code}"), action.endpoints().map { it.path })
    }

    /** The "Endpoint Actions" gutter searches usages and builds the OpenAPI entry from the path it composes itself. */
    fun testEndpointActionsGutterUsesTheResolvedPath() {
        addConfiguration("application.yml", "app:\n  shortener-path: /l\n")
        addController()
        val method = JavaPsiFacade.getInstance(project)
            .findClass("com.example.RedirectController", GlobalSearchScope.allScope(project))!!
            .findMethodsByName("redirect", false).single()

        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        val handler = markers.single().navigationHandler as EndpointIconGutterHandler

        assertEquals("/l/{code}", handler.endpointInfo.path)
    }

    private fun redirectEndpoint(): EndpointElement =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module, listOf(EndpointType.SPRING_MVC))
            .single { it.pathTemplate.endsWith("/{code}") }

    private fun addConfiguration(name: String, text: String) = myFixture.addFileToProject(name, text).also {
        assertNotNull(
            "The fixture configuration must be visible to the property search",
            FoldedPropertyValue.resolve(module, "app.shortener-path")
        )
    }

    private fun addController(prefix: String = "\${app.shortener-path:/d}") {
        myFixture.addFileToProject(
            "com/example/RedirectController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("$prefix")
            public class RedirectController {
                @GetMapping("/{code}")
                public String redirect(@PathVariable("code") String code) {
                    return code;
                }
            }
            """.trimIndent()
        )
    }
}
