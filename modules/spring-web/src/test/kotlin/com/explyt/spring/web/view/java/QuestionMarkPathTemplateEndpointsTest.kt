/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointElementViewData
import com.explyt.spring.web.view.EndpointsTreeData
import com.explyt.spring.web.view.EndpointsViewFilter
import com.intellij.psi.PsiMember

class QuestionMarkPathTemplateEndpointsTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springTest_6_0_7,
    )

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/VanityController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class VanityController {
                @GetMapping("/my/{vanityName:(?!admin).*}") public String vanity() { return ""; }
                @GetMapping("/files/{name:.+?}") public String file() { return ""; }
                @GetMapping("/plain/ping") public String ping() { return ""; }
            }
            """.trimIndent()
        )
    }

    fun testSearchFindsLookaheadTemplate() {
        assertEquals(listOf("/my/{vanityName:(?!admin).*}"), search("/my/john"))
    }

    fun testSearchFindsLazyQuantifierTemplate() {
        assertEquals(listOf("/files/{name:.+?}"), search("/files/report.pdf"))
    }

    fun testSearchFindsPlainEndpoint() {
        assertEquals(listOf("/plain/ping"), search("/plain/ping"))
    }

    fun testReferenceResolvesLookaheadTemplate() {
        assertEquals(listOf("vanity"), resolvedNames("/my/jo<caret>hn"))
    }

    fun testReferenceResolvesLazyQuantifierTemplate() {
        assertEquals(listOf("file"), resolvedNames("/files/report<caret>.pdf"))
    }

    fun testReferenceResolvesPlainEndpoint() {
        assertEquals(listOf("ping"), resolvedNames("/plain/pi<caret>ng"))
    }

    private fun search(text: String): List<String> {
        val rows = rows()
        assertEquals(3, rows.size)
        val found = runCatching {
            EndpointsViewFilter.apply(rows, text, emptySet(), emptySet(), EndpointsViewFilter.declaredBasePaths())
        }
        assertNull("Endpoints search for $text threw", found.exceptionOrNull())
        return found.getOrThrow().map { it.path }
    }

    private fun rows(): List<EndpointElementViewData> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.containingClass?.name == "VanityController" }
            .flatMap(EndpointsTreeData::rowsOf)

    private fun resolvedNames(url: String): List<String> {
        myFixture.configureByText(
            "VanityControllerTest.java", """
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

            class VanityControllerTest {
                void test() {
                    MockMvcRequestBuilders.get("$url");
                }
            }
            """.trimIndent()
        )
        val reference = file.findTypedReferenceAt<ExplytControllerMethodReference>(myFixture.caretOffset)
        assertNotNull("The URL must carry an endpoint reference", reference)
        val resolved = runCatching { reference!!.multiResolve(false) }
        assertNull("Resolving $url threw", resolved.exceptionOrNull())
        return resolved.getOrThrow().mapNotNull { (it.element as? PsiMember)?.name }.sorted()
    }
}
