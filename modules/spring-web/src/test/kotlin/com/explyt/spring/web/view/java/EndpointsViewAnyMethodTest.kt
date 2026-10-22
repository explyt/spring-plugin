/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.references.EndpointRenderer
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointElementViewData
import com.explyt.spring.web.view.EndpointsTreeData
import com.explyt.spring.web.view.EndpointsViewFilter
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation

class EndpointsViewAnyMethodTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
    )

    fun testBareRequestMappingIsOneAllRow() {
        val endpoint = anyMethodEndpoint()

        assertEquals(listOf("ALL"), EndpointsTreeData.rowVerbsOf(endpoint))
        assertEquals(listOf("ALL"), EndpointsTreeData.rowsOf(endpoint).map { it.method })
    }

    fun testAllRowPassesEveryVerbFilter() {
        val rows = EndpointsTreeData.rowsOf(anyMethodEndpoint())

        for (verb in listOf("GET", "POST", "OPTIONS")) {
            assertEquals(verb, listOf("ALL"), byVerbs(rows, setOf(verb)).map { it.method })
        }
    }

    fun testVerblessHttpExchangeClientMethodIsOneAllRowKeptByAVerbFilter() {
        myFixture.addFileToProject(
            "com/example/AnyClient.java", """
            package com.example;

            import org.springframework.web.service.annotation.HttpExchange;

            public interface AnyClient {
                @HttpExchange("/x") String any();
            }
            """.trimIndent()
        )
        val endpoint = endpointsOf("AnyClient").single()
        assertEquals("/x", endpoint.path)
        assertEquals(EndpointType.SPRING_HTTP_EXCHANGE, endpoint.type)
        assertEquals(emptyList<String>(), endpoint.requestMethods)

        assertEquals(listOf("ALL"), EndpointsTreeData.rowVerbsOf(endpoint))
        assertEquals(listOf("ALL"), byVerbs(EndpointsTreeData.rowsOf(endpoint), setOf("PUT")).map { it.method })
    }

    fun testGetMappingRowIsRemovedByAnotherVerbFilter() {
        val endpoint = getEndpoint()

        assertEquals(listOf("GET"), EndpointsTreeData.rowVerbsOf(endpoint))
        val rows = EndpointsTreeData.rowsOf(endpoint)
        assertEquals(emptyList<String>(), byVerbs(rows, setOf("POST")).map { it.method })
        assertEquals(listOf("GET"), byVerbs(rows, setOf("GET")).map { it.method })
    }

    fun testVerbFilterKeepsTheAllRowAndDropsTheListedSiblingOnTheSamePath() {
        myFixture.addFileToProject(
            "com/example/MixedController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class MixedController {
                @RequestMapping("/m") public String any() { return "any"; }
                @PostMapping("/m") public String post() { return "post"; }
            }
            """.trimIndent()
        )
        val endpoints = endpointsOf("MixedController")
        assertEquals(listOf("/m", "/m"), endpoints.map { it.path })
        assertEquals(listOf("", "POST"), endpoints.map { it.requestMethods.joinToString() }.sorted())

        val rows = endpoints.flatMap(EndpointsTreeData::rowsOf)
        assertEquals(listOf("ALL"), byVerbs(rows, setOf("GET")).map { it.method })
    }

    fun testTextAndTypeFiltersStillExcludeAnAnyMethodRow() {
        val rows = EndpointsTreeData.rowsOf(anyMethodEndpoint())
        val basePaths = EndpointsViewFilter.declaredBasePaths()

        assertEquals(emptyList<EndpointElementViewData>(), EndpointsViewFilter.apply(rows, "nomatch", emptySet(), emptySet(), basePaths))
        assertEquals(
            emptyList<EndpointElementViewData>(),
            EndpointsViewFilter.apply(rows, "", emptySet(), setOf(EndpointType.SPRING_WEBFLUX), basePaths)
        )
        assertEquals(
            emptyList<EndpointElementViewData>(),
            EndpointsViewFilter.apply(rows, "", setOf("GET"), setOf(EndpointType.SPRING_WEBFLUX), basePaths)
        )
    }

    fun testCompletionRendersAnAnyMethodEndpointAsAll() {
        assertEquals("[ALL] AnyController", typeTextOf(anyMethodEndpoint()))
    }

    fun testCompletionRendersAListedEndpointWithItsVerb() {
        assertEquals("[GET] GetController", typeTextOf(getEndpoint()))
    }

    private fun anyMethodEndpoint(): EndpointElement {
        myFixture.addFileToProject(
            "com/example/AnyController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class AnyController {
                @RequestMapping("/x") public String any() { return "any"; }
            }
            """.trimIndent()
        )
        val endpoint = endpointsOf("AnyController").single()
        assertEquals("/x", endpoint.path)
        assertEquals(EndpointType.SPRING_MVC, endpoint.type)
        assertEquals(emptyList<String>(), endpoint.requestMethods)
        return endpoint
    }

    private fun getEndpoint(): EndpointElement {
        myFixture.addFileToProject(
            "com/example/GetController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.*;

            @RestController
            public class GetController {
                @GetMapping("/y") public String get() { return "get"; }
            }
            """.trimIndent()
        )
        val endpoint = endpointsOf("GetController").single()
        assertEquals("/y", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
        return endpoint
    }

    private fun endpointsOf(className: String): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.containingClass?.name == className }

    private fun byVerbs(rows: List<EndpointElementViewData>, verbs: Set<String>) =
        EndpointsViewFilter.apply(rows, "", verbs, emptySet(), EndpointsViewFilter.declaredBasePaths())

    private fun typeTextOf(endpoint: EndpointElement): String? {
        val presentation = LookupElementPresentation()
        EndpointRenderer().renderElement(LookupElementBuilder.create(endpoint, endpoint.path), presentation)
        return presentation.typeText
    }
}
