/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.util.ApplicationBasePath
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import kotlinx.coroutines.runBlocking

/**
 * What the endpoint tools state about the paths a module's configuration declares: a base path in front of every
 * mapping, and a property placeholder inside a mapping.
 *
 * A declared base path is a fact about the application, and reporting it as the guessed `assumedPrefix` left the
 * caller unable to tell a verified context path from a segment the tool merely dropped. A resolved placeholder is
 * what the application serves, but the declaration is what a caller edits, so the tools report both.
 */
class SpringBootApplicationMcpToolsetDeclaredPathTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    private fun projectPath(): String = project.basePath ?: ""

    private suspend fun find(url: String, httpMethod: String = ""): JsonNode =
        mapper.readTree(toolset.findEndpoint(urlPattern = url, projectPath = projectPath(), httpMethod = httpMethod))

    private fun paths(nodes: JsonNode): List<String> = nodes.map { it["fullPath"].asText() }

    private fun declareConfiguration(yaml: String, expectedBasePath: String) {
        myFixture.copyDirectoryToProject("springBootApp", "")
        assertNotNull(
            "The fixture must be a servlet application, or no servlet base path applies",
            JavaPsiFacade.getInstance(project).findClass(
                "org.springframework.web.servlet.DispatcherServlet", module.moduleWithLibrariesScope
            )
        )
        myFixture.addFileToProject("application.yml", yaml)
        assertEquals("The configuration must declare the base path", expectedBasePath, ApplicationBasePath.of(module))
    }

    fun testDeclaredContextPathIsReportedAsBasePath() = runBlocking<Unit> {
        declareConfiguration("server:\n  servlet:\n    context-path: /t\n", expectedBasePath = "/t")

        val root = find("https://example.com/t/api/demo/items/42?window=24h")

        assertEquals(listOf("/api/demo/items/{id}"), paths(root["endpoints"]))
        assertEquals("/t", root["basePath"].asText())
        assertTrue("A declared base path is not a guess, got $root", root["assumedPrefix"].isNull)
    }

    /**
     * Spring MVC serves under the context path and then under the dispatcher servlet path, so a request carries
     * both. Reporting only the context path - or guessing the rest - would misstate what the application declares.
     */
    fun testContextPathAndServletPathAreStrippedTogether() = runBlocking<Unit> {
        declareConfiguration(
            "server:\n  servlet:\n    context-path: /t\nspring:\n  mvc:\n    servlet:\n      path: /web\n",
            expectedBasePath = "/t/web",
        )

        val root = find("/t/web/api/demo/items/42")

        assertEquals(listOf("/api/demo/items/{id}"), paths(root["endpoints"]))
        assertEquals("/t/web", root["basePath"].asText())
        assertTrue(root["assumedPrefix"].isNull)
    }

    /** The contract tool looks a URL up the same way, so it reports the same base path. */
    fun testContractReportsTheDeclaredBasePath() = runBlocking<Unit> {
        declareConfiguration("server:\n  servlet:\n    context-path: /t\n", expectedBasePath = "/t")

        val root = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/t/api/demo/items/42", projectPath = projectPath(), httpMethod = "GET")
        )

        assertEquals("/t", root["basePath"].asText())
        assertTrue(root["assumedPrefix"].isNull)
        assertEquals("COMPLETE", root["endpoints"].single()["contractStatus"].asText())
    }

    /**
     * A URL a route answers as written is answered so, even when a base path is declared: the caller may have
     * written the mapping path, not the deployed URL, and stripping nothing is the literal reading.
     */
    fun testUrlMatchingAsWrittenWinsOverTheBasePath() = runBlocking<Unit> {
        declareConfiguration("server:\n  servlet:\n    context-path: /api\n", expectedBasePath = "/api")

        val root = find("/api/demo/items/42")

        assertEquals(listOf("/api/demo/items/{id}"), paths(root["endpoints"]))
        assertTrue("Nothing was stripped, got $root", root["basePath"].isNull)
        assertTrue(root["assumedPrefix"].isNull)
    }

    /**
     * A declared base path is stripped only at a segment boundary: `/tapi` is not `/t` followed by `api`, and reading
     * it so would report a base path the URL does not carry and a route it does not address.
     */
    fun testBasePathIsStrippedOnlyAtASegmentBoundary() = runBlocking<Unit> {
        declareConfiguration("server:\n  servlet:\n    context-path: /t\n", expectedBasePath = "/t")

        val root = find("/tapi/demo/items/42")

        assertTrue("'/tapi' does not open with the base path '/t', got $root", root["basePath"].isNull)
        assertEquals("'/tapi/demo/items/42' names no route, got $root", 0, root["totalCount"].asInt())
    }

    /**
     * A miss under a declared base path is answered with the neighbourhood read under it: the caller is looking for
     * where to add a route to this application, and the routes it serves under the base path are that place.
     */
    fun testMissUnderTheBasePathListsTheNearestRoutes() = runBlocking<Unit> {
        declareConfiguration("server:\n  servlet:\n    context-path: /t\n", expectedBasePath = "/t")

        val root = find("/t/api/routes/export/preview")

        assertEquals(0, root["totalCount"].asInt())
        assertEquals("/t", root["basePath"].asText())
        assertTrue(root["assumedPrefix"].isNull)
        assertEquals("/api/routes/export", root["sharedPrefix"].asText())
    }

    /**
     * The endpoint is matched and listed by the path the application serves, and the declaration a caller edits is
     * reported beside it - in every endpoint tool, since each one names the endpoint.
     */
    fun testResolvedPlaceholderKeepsItsDeclarationInEveryTool() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        addPlaceholderController()
        myFixture.addFileToProject("application.yml", "app:\n  shortener-path: /l\n")

        val found = find("/l/offer")["endpoints"].single()
        assertEquals("/l/{code}", found["fullPath"].asText())
        assertEquals("/\${app.shortener-path:/x}/{code}", found["pathTemplate"].asText())

        val contract = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/l/offer", projectPath = projectPath(), httpMethod = "GET")
        )["endpoints"].single()
        assertEquals("/l/{code}", contract["fullPath"].asText())
        assertEquals("/\${app.shortener-path:/x}/{code}", contract["pathTemplate"].asText())

        for (compact in listOf(false, true)) {
            val listed = mapper.readTree(
                toolset.getHttpEndpoints(
                    projectPath = projectPath(), controllerFilter = "ShortLinkRedirectController", compact = compact
                )
            )["endpoints"].single()
            assertEquals("compact=$compact", "/l/{code}", listed["fullPath"].asText())
            assertEquals("compact=$compact", "/\${app.shortener-path:/x}/{code}", listed["pathTemplate"].asText())
        }
    }

    /**
     * An endpoint declared without a placeholder keeps its shape: `pathTemplate` would only repeat `fullPath`, and
     * its presence is what tells a caller that the declaration differs from the served path.
     */
    fun testEndpointWithoutPlaceholderHasNoPathTemplate() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val found = find("/api/demo/items/42")["endpoints"].single()
        val listed = mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), controllerFilter = "DemoController", compact = true)
        )["endpoints"]

        assertFalse("No pathTemplate key for a literal mapping, got $found", found.has("pathTemplate"))
        assertTrue(listed.none { it.has("pathTemplate") })
    }

    private fun addPlaceholderController() {
        myFixture.addFileToProject(
            "com/example/app/web/ShortLinkRedirectController.java", """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("${'$'}{app.shortener-path:/x}")
            public class ShortLinkRedirectController {
                @GetMapping("/{code}")
                public String redirect(@PathVariable("code") String code) {
                    return code;
                }
            }
            """.trimIndent()
        )
    }
}
