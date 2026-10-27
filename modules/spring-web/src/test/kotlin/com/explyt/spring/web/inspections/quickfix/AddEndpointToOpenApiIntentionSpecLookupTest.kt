/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.quickfix

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.providers.ControllerEndpointActionsLineMarkerProvider
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.explyt.spring.web.providers.EndpointUsageSearcher
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PlatformTestUtil

class AddEndpointToOpenApiIntentionSpecLookupTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7
    )

    fun testJsonSpecDocumentingTheEndpointHidesTheAction() {
        addFileToModule(module, "openapi.json", jsonSpec("/item"))
        val intention = AddEndpointToOpenApiIntention(itemEndpoint())

        assertEquals(
            "Precondition: the JSON spec documents GET /item",
            1, EndpointUsageSearcher.findOpenApiJsonEndpoints("/item", listOf("GET"), module).size
        )
        assertFalse(intention.isAvailable(project, null, null))
    }

    fun testYamlSpecDocumentingTheEndpointHidesTheAction() {
        addFileToModule(module, "openapi.yaml", YAML_SPEC)
        val intention = AddEndpointToOpenApiIntention(itemEndpoint())

        assertEquals(
            "Precondition: the YAML spec documents GET /item",
            1, EndpointUsageSearcher.findOpenApiYamlEndpoints("/item", listOf("GET"), module).size
        )
        assertFalse(intention.isAvailable(project, null, null))
    }

    fun testActionAddsTheEndpointToTheOnlyJsonSpec() {
        val spec = addFileToModule(module, "openapi.json", jsonSpec("/other")).virtualFile
        val intention = AddEndpointToOpenApiIntention(itemEndpoint())
        assertEquals(
            "Precondition: the JSON spec is the only OpenAPI spec of the project",
            listOf(spec),
            (EndpointUsageSearcher.findOpenApiJsonData(project) + EndpointUsageSearcher.findOpenApiYamlData(project))
                .map { it.psiFile.virtualFile }
        )
        assertTrue("Precondition: GET /item is not documented yet", intention.isAvailable(project, null, null))

        intention.invoke(project, null, null)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        val specFile = PsiManager.getInstance(project).findFile(spec)!!
        val text = specFile.text
        val createdYaml = ModuleRootManager.getInstance(module).contentRoots.first()
            .findFileByRelativePath("src/main/resources/static/openapi.yml")
        assertNull(
            "No YAML spec may be created next to the JSON one, got ${createdYaml?.path}",
            createdYaml
        )
        assertEquals(
            "The endpoint must be added to the only spec of the module, which is JSON:\n$text",
            1, text.split("\"/item\"").size - 1
        )
        assertFalse("The JSON spec must stay valid:\n$text", PsiTreeUtil.hasErrorElements(specFile))
    }

    private fun itemEndpoint(): AddEndpointToOpenApiIntention.EndpointInfo {
        addFileToModule(
            module, "com/example/ItemController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class ItemController {
                @GetMapping("/item")
                public String item() { return "item"; }
            }
            """.trimIndent()
        )
        val method = JavaPsiFacade.getInstance(project)
            .findClass("com.example.ItemController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("item", false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        val endpoint = (markers.single().navigationHandler as EndpointIconGutterHandler).endpointInfo
        assertEquals("/item", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
        return endpoint
    }

    private fun jsonSpec(path: String) = """
        {
          "openapi": "3.0.3",
          "info": {
            "title": "Items",
            "version": "1.0.0"
          },
          "paths": {
            "$path": {
              "get": {
                "operationId": "documented"
              }
            }
          }
        }
    """.trimIndent()
}

private val YAML_SPEC = """
    openapi: 3.0.1
    info:
      title: Items
      version: 1.0.0
    paths:
      /item:
        get:
          operationId: documented
""".trimIndent()
