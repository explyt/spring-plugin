/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.providers.ControllerEndpointActionsLineMarkerProvider
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.explyt.spring.web.providers.EndpointUsageSearcher
import com.intellij.codeInsight.daemon.LineMarkerInfo
import org.jetbrains.kotlin.psi.KtNamedFunction

class VerblessEndpointOpenApiActionTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7
    )

    fun testVerblessMappingGutterOffersAnOpenApiDescription() {
        addController()

        val handler = gutterHandlerOf("items")
        assertEquals("/items", handler.endpointInfo.path)
        assertEquals("Precondition: the mapping declares no method", emptyList<String>(), handler.endpointInfo.requestMethods)

        assertTrue(
            "A mapping that answers every method must offer Generate OpenApi Doc",
            offersOpenApiDescription(handler)
        )
        assertTrue(AddEndpointToOpenApiIntention(handler.endpointInfo).isAvailable(project, null, null))
    }

    fun testGetMappingGutterStillOffersAnOpenApiDescription() {
        addController()

        val handler = gutterHandlerOf("item")
        assertEquals("/item", handler.endpointInfo.path)
        assertEquals(listOf("GET"), handler.endpointInfo.requestMethods)

        assertTrue(offersOpenApiDescription(handler))
        assertTrue(AddEndpointToOpenApiIntention(handler.endpointInfo).isAvailable(project, null, null))
    }

    fun testVerblessMappingIntentionIsAvailableWhileAnOperationIsUndocumented() {
        myFixture.addFileToProject("openapi.yaml", DOCUMENTED_ITEMS)
        addController()

        val handler = gutterHandlerOf("items")
        assertEquals(
            "Precondition: the spec documents GET and POST /items",
            2, EndpointUsageSearcher.findOpenApiYamlEndpoints("/items", listOf("GET", "POST"), module).size
        )

        assertTrue(
            "GET and POST are documented, the five other operations of a mapping that answers every method are not",
            AddEndpointToOpenApiIntention(handler.endpointInfo).isAvailable(project, null, null)
        )
    }

    private fun addController() {
        myFixture.addFileToProject(
            "com/example/ItemController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class ItemController {
                @RequestMapping("/items")
                fun items(): String = "items"

                @GetMapping("/item")
                fun item(): String = "item"
            }
            """.trimIndent()
        )
    }

    private fun gutterHandlerOf(methodName: String): EndpointIconGutterHandler {
        val method = myFixture.findClass("com.example.ItemController").findMethodsByName(methodName, false).single()
        val identifier = (method.navigationElement as KtNamedFunction).nameIdentifier!!
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(identifier), markers)
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
