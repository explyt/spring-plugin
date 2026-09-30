/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
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
