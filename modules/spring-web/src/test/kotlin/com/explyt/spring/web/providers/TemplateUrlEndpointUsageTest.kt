/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

/**
 * The tests that send a request to an endpoint, found by the URL they build.
 *
 * A test rarely writes its URL as one constant: it interpolates the id it just created, or wraps the text in a
 * `java.net.URI`. Such a request was taken for a request to no endpoint, so the endpoint gutter and the call trace of
 * the MCP tools listed no test for it. Every part known only at run time now stands for one path segment.
 */
class TemplateUrlEndpointUsageTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/ItemController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            @RequestMapping("/api/items")
            public class ItemController {
                @GetMapping("/{id}") public String get(@PathVariable String id) { return id; }
                @GetMapping("/{id}/history/{page}") public String history(@PathVariable String id, @PathVariable int page) { return id; }
            }
            """.trimIndent()
        )
    }

    fun testKotlinTemplatesAndUriObjectsAddressTheEndpoint() {
        myFixture.configureByText(
            "ItemControllerTest.kt", """
            import java.net.URI
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class ItemControllerTest {
                private val base = "/api/items"

                fun simpleTemplate(id: Long) { get("/api/items/${'$'}id") }
                fun blockTemplate(item: Item) { get("/api/items/${'$'}{item.id}") }
                fun interpolatedBase(id: Long) { get("${'$'}{base}/${'$'}id") }
                fun uriCreate(id: Long) { get(URI.create("http://localhost:8080/api/items/${'$'}id?x=1")) }
                fun uriConstructor(id: Long) { get(URI("/api/items/${'$'}id")) }
                fun otherRoute(id: Long) { get("/other/${'$'}id") }
                fun otherHost(id: Long) { get(URI.create("https://payments.example.com/api/items/${'$'}id")) }
                fun constant() { get("/api/items/42") }
            }

            class Item(val id: Long)
            """.trimIndent()
        )

        assertEquals(
            listOf("blockTemplate", "constant", "interpolatedBase", "simpleTemplate", "uriConstructor", "uriCreate"),
            callersOf("/api/items/{id}")
        )
    }

    fun testJavaConcatenationAddressesTheEndpoint() {
        myFixture.configureByText(
            "ItemControllerTest.java", """
            import java.net.URI;
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class ItemControllerTest {
                void concatenation(long id) { MockMvcRequestBuilders.get("/api/items/" + id); }
                void uriCreate(long id) { MockMvcRequestBuilders.get(URI.create("/api/items/" + id)); }
                void newUri(long id) throws Exception { MockMvcRequestBuilders.get(new URI("/api/items/" + id)); }
                void otherRoute(long id) { MockMvcRequestBuilders.get("/other/" + id); }
                void relative() { MockMvcRequestBuilders.get("/api/items/{id}", 42); }
            }
            """.trimIndent()
        )

        assertEquals(listOf("concatenation", "newUri", "relative", "uriCreate"), callersOf("/api/items/{id}"))
    }

    /** One interpolated value is one segment: it cannot stand for the two a longer route needs. */
    fun testVariablePartIsOneSegment() {
        myFixture.configureByText(
            "ItemControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class ItemControllerTest {
                fun onePart(id: Long) { get("/api/items/${'$'}id") }
                fun twoParts(id: Long, page: Int) { get("/api/items/${'$'}id/history/${'$'}page") }
            }
            """.trimIndent()
        )

        assertEquals(listOf("twoParts"), callersOf("/api/items/{id}/history/{page}"))
    }

    /** A URL known only at run time says nothing about the endpoint, even the one whose whole route is a template. */
    fun testUrlWithNoLiteralPartAddressesNoEndpoint() {
        myFixture.addFileToProject(
            "com/example/RedirectController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class RedirectController {
                @GetMapping("/{code}") public String redirect(@PathVariable String code) { return code; }
            }
            """.trimIndent()
        )
        myFixture.configureByText(
            "ItemControllerTest.kt", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class ItemControllerTest {
                fun unknown(url: String) { get(url) }
                fun interpolatedOnly(code: String) { get("/${'$'}code") }
                fun literal() { get("/abc") }
            }
            """.trimIndent()
        )

        assertEquals(listOf("literal"), callersOf("/{code}"))
    }

    fun testWebTestClientTemplateAddressesTheEndpoint() {
        myFixture.configureByText(
            "ItemControllerTest.kt", """
            import org.springframework.test.web.reactive.server.WebTestClient

            class ItemControllerTest {
                fun template(client: WebTestClient, id: Long) { client.get().uri("/api/items/${'$'}id") }
                fun otherRoute(client: WebTestClient, id: Long) { client.get().uri("/other/${'$'}id") }
            }
            """.trimIndent()
        )

        val callers = EndpointUsageSearcher.findWebTestClientEndpointUsage("/api/items/{id}", listOf("GET"), module)
            .map(::enclosingMethodName)
            .sorted()
        assertEquals(listOf("template"), callers)
    }

    private fun callersOf(endpoint: String): List<String> =
        EndpointUsageSearcher.findMockMvcEndpointUsage(endpoint, listOf("GET"), module)
            .map(::enclosingMethodName)
            .sorted()

    private fun enclosingMethodName(element: PsiElement): String =
        element.toUElement()?.getParentOfType<UMethod>()?.name
            ?: PsiTreeUtil.getParentOfType(element, PsiNamedElement::class.java)?.name
            ?: "?"
}
