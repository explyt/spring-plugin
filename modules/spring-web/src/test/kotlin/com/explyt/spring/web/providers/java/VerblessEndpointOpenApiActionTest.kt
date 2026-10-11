/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.providers.ControllerEndpointActionsLineMarkerProvider
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.explyt.spring.web.providers.EndpointUsageSearcher
import com.intellij.codeInsight.daemon.LineMarkerInfo

class VerblessEndpointOpenApiActionTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7
    )

    fun testVerblessMappingGutterDoesNotOfferAnOpenApiDescription() {
        addController()

        val handler = gutterHandlerOf("items")
        assertEquals("/items", handler.endpointInfo.path)
        assertEquals("Precondition: the mapping declares no method", emptyList<String>(), handler.endpointInfo.requestMethods)

        assertFalse(
            "A mapping without a method must not offer Generate OpenApi Doc",
            offersOpenApiDescription(handler)
        )
    }

    fun testGetMappingGutterStillOffersAnOpenApiDescription() {
        addController()

        val handler = gutterHandlerOf("item")
        assertEquals("/item", handler.endpointInfo.path)
        assertEquals(listOf("GET"), handler.endpointInfo.requestMethods)

        assertTrue(offersOpenApiDescription(handler))
        assertTrue(AddEndpointToOpenApiIntention(handler.endpointInfo).isAvailable(project, null, null))
    }

    fun testVerblessMappingIntentionIsNotAvailableForADocumentedPath() {
        myFixture.addFileToProject("openapi.yaml", DOCUMENTED_ITEMS)
        addController()

        val handler = gutterHandlerOf("items")
        assertEquals(
            "Precondition: the spec documents GET and POST /items",
            2, EndpointUsageSearcher.findOpenApiYamlEndpoints("/items", listOf("GET", "POST"), module).size
        )

        assertFalse(
            "Generate OpenApi Doc must not be available for a mapping without a method",
            AddEndpointToOpenApiIntention(handler.endpointInfo).isAvailable(project, null, null)
        )
    }

    private fun addController() {
        myFixture.addFileToProject(
            "com/example/ItemController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class ItemController {
                @RequestMapping("/items")
                public String items() { return "items"; }

                @GetMapping("/item")
                public String item() { return "item"; }
            }
            """.trimIndent()
        )
    }

    private fun gutterHandlerOf(methodName: String): EndpointIconGutterHandler {
        val method = myFixture.findClass("com.example.ItemController").findMethodsByName(methodName, false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        return markers.single().navigationHandler as EndpointIconGutterHandler
    }

    private fun offersOpenApiDescription(handler: EndpointIconGutterHandler): Boolean =
        EndpointIconGutterHandler::class.java.getDeclaredField("offersOpenApiDescription")
            .apply { isAccessible = true }
            .getBoolean(handler)
}

private val DOCUMENTED_ITEMS = """
    openapi: 3.0.1
    info:
      title: Items
      version: 1.0.0
    paths:
      /items:
        get:
          operationId: listItems
        post:
          operationId: createItem
""".trimIndent()
