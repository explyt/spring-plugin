/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.toSmartPointer

/**
 * What the Endpoints tool window shows for a search text. A pasted URL or a concrete path used to find nothing: the
 * search was a plain `contains`, and `/api/items/42` is contained in no `{template}`.
 */
class EndpointsViewFilterTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
    )

    private val rows: List<EndpointElementViewData> by lazy { loadRows() }

    fun testControllerCaptureRestMatchesManySegments() {
        val rows = fileControllerRows()
        val capture = rows.single { it.path == "/files/{*path}" }

        assertTrue(SpringWebUtil.isEndpointMatches(capture.path, "/files/a/b/c"))
    }

    fun testControllerDoubleWildcardMatchesManySegments() {
        val wildcard = fileControllerRows().single { it.path == "/static/**" }

        assertTrue(SpringWebUtil.isEndpointMatches(wildcard.path, "/static/a/b"))
    }

    fun testControllerDoubleWildcardMatchesZeroSegments() {
        val wildcard = fileControllerRows().single { it.path == "/static/**" }

        assertTrue(SpringWebUtil.isEndpointMatches(wildcard.path, "/static"))
    }

    fun testToolWindowFindsControllerCaptureRestForANestedPath() {
        val rows = fileControllerRows()
        val matched = EndpointsViewFilter.apply(
            rows, "/files/a/b/c", emptySet(), emptySet(), EndpointsViewFilter.declaredBasePaths()
        )

        assertEquals(listOf("/files/{*path}"), matched.map { it.path })
    }

    private fun fileControllerRows(): List<EndpointElementViewData> {
        myFixture.addFileToProject(
            "com/example/FileController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.*;

            @RestController
            public class FileController {
                @GetMapping("/files/{*path}") public String files() { return "files"; }
                @GetMapping("/static/**") public String resources() { return "resources"; }
            }
            """.trimIndent()
        )
        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.containingClass?.name == "FileController" }
        assertEquals(listOf("/files/{*path}", "/static/**"), endpoints.map { it.path }.sorted())
        assertTrue(endpoints.all { it.type == EndpointType.SPRING_MVC })
        return endpoints.flatMap(EndpointsTreeData::rowsOf)
    }

    private fun rows(): List<EndpointElementViewData> = rows

    private fun loadRows(): List<EndpointElementViewData> {
        myFixture.addFileToProject(
            "com/example/ItemController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/items")
            public class ItemController {
                @GetMapping("/{id}") public String get(@PathVariable String id) { return id; }
                @PutMapping("/{id}") public String put(@PathVariable String id) { return id; }
                @GetMapping("/export") public String export() { return ""; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ArchiveController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class ArchiveController {
                @DeleteMapping("/items/export") public void purge() { }
            }
            """.trimIndent()
        )
        return SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .flatMap { endpoint ->
                endpoint.requestMethods.map { method ->
                    EndpointElementViewData(
                        endpoint.type, endpoint.psiElement.toSmartPointer(), "ItemController", method, endpoint.path
                    )
                }
            }
    }

    private fun filter(text: String, methods: Set<String> = emptySet()) =
        EndpointsViewFilter.apply(rows(), text, methods, emptySet(), EndpointsViewFilter.declaredBasePaths())
            .map { "${it.method} ${it.path}" }

    fun testPastedUrlFindsTheTemplateRoute() {
        assertEquals(
            listOf("GET /api/items/{id}", "PUT /api/items/{id}"),
            filter("https://example.com/api/items/42?expand=true").sorted()
        )
    }

    /** The literal route Spring dispatches `GET .../export` to comes first; the `{id}` route matches the URL too. */
    fun testDeclaredContextPathIsStripped() {
        myFixture.addFileToProject("application.properties", "server.servlet.context-path=/t\n")

        assertEquals(
            listOf("GET /api/items/export", "GET /api/items/{id}", "PUT /api/items/{id}"),
            filter("http://localhost:8080/t/api/items/export")
        )
    }

    /**
     * The declared base path is a fact, so it outranks a guess: under `/t/api` the URL is `/items/export`. Guessing
     * would drop only `/t` and show the `/api/items/...` routes, which this application serves at `/t/api/api/items`.
     */
    fun testDeclaredBasePathOutranksAGuess() {
        myFixture.addFileToProject(
            "application.properties", "server.servlet.context-path=/t\nspring.mvc.servlet.path=/api\n"
        )

        assertEquals(listOf("DELETE /items/export"), filter("https://example.com/t/api/items/export"))
    }

    fun testUndeclaredPrefixIsGuessed() {
        assertEquals(
            listOf("GET /api/items/{id}", "PUT /api/items/{id}"),
            filter("/gateway/api/items/42").sorted()
        )
    }

    /**
     * A verb filter narrows what the URL found. Applied first, it would push `/api/items/export` under the guessed
     * prefix `/api` and show `DELETE /items/export`, a different URL.
     */
    fun testMethodFilterAppliesAfterTheUrl() {
        assertEquals(emptyList<String>(), filter("/api/items/export", methods = setOf("DELETE")))
        assertEquals(listOf("DELETE /items/export"), filter("/items/export", methods = setOf("DELETE")))
    }

    fun testTextWithoutASlashStaysAFragmentSearch() {
        assertEquals(listOf("DELETE /items/export", "GET /api/items/export"), filter("xpor").sorted())
    }

    fun testEndpointTypeFilterStillApplies() {
        val rows = rows()
        assertTrue(rows.all { it.type == EndpointType.SPRING_MVC })

        val none = EndpointsViewFilter.apply(
            rows, "/api/items/42", emptySet(), setOf(EndpointType.SPRING_WEBFLUX), EndpointsViewFilter.declaredBasePaths()
        )
        assertEquals(emptyList<EndpointElementViewData>(), none)
    }
}
