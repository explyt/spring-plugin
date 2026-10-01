/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.references

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.providers.EndpointUsageSearcher
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil

/**
 * A URL written in code the way a client sends it - absolute against this machine, or under the context path the
 * application declares - addresses the same endpoint as its path does. It used to resolve to nothing:
 * `http://localhost:8080/api/items/{id}` became `/http:/localhost:8080/...`, and `/t/api/items` was looked up whole.
 */
class AbsoluteUrlEndpointReferenceTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springReactiveWeb_3_1_1,
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
                @GetMapping("/export") public String export() { return ""; }
            }
            """.trimIndent()
        )
    }

    private fun resolvedNames(test: String): List<String> {
        myFixture.configureByText("ItemControllerTest.java", test.trimIndent())
        val reference = file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("The URL must carry an endpoint reference", reference)
        return reference!!.multiResolve(false).mapNotNull { (it.element as? PsiMember)?.name }.sorted()
    }

    fun testMockMvcAbsoluteUrlResolves() {
        val names = resolvedNames(
            """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class ItemControllerTest {
                void test() {
                    MockMvcRequestBuilders.get("http://localhost:8080/api/items/{i<caret>d}", 42);
                }
            }
            """
        )

        assertEquals(listOf("get"), names)
    }

    /** A poly-variant reference lists every route the URL matches, the `{id}` route included, as a relative URL does. */
    fun testWebClientAbsoluteUrlResolves() {
        val names = resolvedNames(
            """
            import org.springframework.web.reactive.function.client.WebClient;

            class ItemControllerTest {
                private WebClient webClient;

                void test() {
                    webClient.get().uri("http://127.0.0.1:8080/api/items/exp<caret>ort");
                }
            }
            """
        )

        assertEquals(listOf("export", "get"), names)
    }

    fun testDeclaredContextPathIsStrippedFromAReference() {
        assertNotNull(
            "The fixture must be a servlet application for server.servlet.context-path to apply",
            com.intellij.psi.JavaPsiFacade.getInstance(project)
                .findClass("org.springframework.web.servlet.DispatcherServlet", module.moduleWithLibrariesScope)
        )
        myFixture.addFileToProject("application.properties", "server.servlet.context-path=/t\n")

        val names = resolvedNames(
            """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class ItemControllerTest {
                void test() {
                    MockMvcRequestBuilders.get("/t/api/items/exp<caret>ort");
                }
            }
            """
        )

        assertEquals(listOf("export", "get"), names)
    }

    /** A prefix nobody declared is no evidence of which endpoint the code means, so a reference never guesses one. */
    fun testUndeclaredPrefixDoesNotResolve() {
        val names = resolvedNames(
            """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class ItemControllerTest {
                void test() {
                    MockMvcRequestBuilders.get("/t/api/items/exp<caret>ort");
                }
            }
            """
        )

        assertEquals(emptyList<String>(), names)
    }

    /** A call to another host is a call to another service, however similar its path. */
    fun testUrlOfAnotherHostDoesNotResolve() {
        val names = resolvedNames(
            """
            import org.springframework.web.reactive.function.client.WebClient;

            class ItemControllerTest {
                private WebClient webClient;

                void test() {
                    webClient.get().uri("https://payments.example.com/api/items/exp<caret>ort");
                }
            }
            """
        )

        assertEquals(emptyList<String>(), names)
    }

    fun testRedirectToAnotherHostStaysUnresolved() {
        val names = resolvedNames(
            """
            class ItemControllerTest {
                String test() {
                    return "redirect:https://payments.example.com/api/items/exp<caret>ort";
                }
            }
            """
        )

        assertEquals(emptyList<String>(), names)
    }

    fun testRelativeUrlKeepsResolving() {
        val names = resolvedNames(
            """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class ItemControllerTest {
                void test() {
                    MockMvcRequestBuilders.get("/api/items/{i<caret>d}", 42);
                }
            }
            """
        )

        assertEquals(listOf("get"), names)
    }

    /** The endpoint gutter lists the tests calling an endpoint; a test written against an absolute URL is one of them. */
    fun testUsageSearchFindsAnAbsoluteUrlMockMvcCall() {
        myFixture.addFileToProject("application.properties", "server.servlet.context-path=/t\n")
        myFixture.configureByText(
            "ItemControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class ItemControllerTest {
                void absolute() { MockMvcRequestBuilders.get("http://localhost:8080/api/items/{id}", 42); }
                void underContextPath() { MockMvcRequestBuilders.get("/t/api/items/7"); }
                void otherHost() { MockMvcRequestBuilders.get("https://payments.example.com/api/items/7"); }
            }
            """.trimIndent()
        )

        val usages = EndpointUsageSearcher.findMockMvcEndpointUsage("/api/items/{id}", listOf("GET"), module)

        val callers = usages.mapNotNull {
            PsiTreeUtil.getParentOfType(it, PsiMethodCallExpression::class.java, false)
                ?.let { call -> PsiTreeUtil.getParentOfType(call, com.intellij.psi.PsiMethod::class.java)?.name }
        }.sorted()
        assertEquals(
            "MockMvc dispatches inside the JVM, so a request naming another host still reaches the controller under test",
            listOf("absolute", "otherHost", "underContextPath"), callers
        )
    }
}
