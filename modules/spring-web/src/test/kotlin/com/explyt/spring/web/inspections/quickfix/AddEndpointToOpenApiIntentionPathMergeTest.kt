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
import com.intellij.lang.Language
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

class AddEndpointToOpenApiIntentionPathMergeTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7
    )

    fun testYamlActionKeepsTheDocumentedOperationOfThePath() {
        val spec = addSpec("openapi.yaml", YAML_POST_ITEMS)
        assertPostItemsIsDocumentedAndGetIsMissing()

        val text = applyActionFor(itemsEndpoint(), spec)

        val items = yamlPathItems(text, "/items")
        assertEquals("The spec must keep exactly one /items path item:\n$text", 1, items.size)
        val operations = items.single().value as YAMLMapping
        assertEquals(
            "The documented POST /items must survive next to the added GET:\n$text",
            setOf("get", "post"), operations.keyValues.mapNotNull { it.keyText }.toSet()
        )
        val post = operations.getKeyValueByKey("post")!!.value as YAMLMapping
        assertEquals("createItem", post.getKeyValueByKey("operationId")?.valueText)
        assertEquals("Create an item", post.getKeyValueByKey("summary")?.valueText)
        assertEquals(
            "The plugin must read the written spec as documenting GET and POST /items:\n$text",
            setOf("get", "post"),
            EndpointUsageSearcher.findOpenApiYamlEndpoints("/items", listOf("GET", "POST"), module)
                .map { (it as YAMLKeyValue).keyText }.toSet()
        )
    }

    fun testJsonActionKeepsTheDocumentedOperationOfThePath() {
        val spec = addSpec("openapi.json", JSON_POST_ITEMS)
        assertPostItemsIsDocumentedAndGetIsMissing()

        val text = applyActionFor(itemsEndpoint(), spec)

        val items = jsonPathItems(text, "/items")
        assertEquals("The spec must keep exactly one \"/items\" key:\n$text", 1, items.size)
        val operations = items.single()
        assertEquals(
            "The documented POST /items must survive next to the added GET:\n$text",
            setOf("get", "post"), operations.propertyList.map { it.name }.toSet()
        )
        val post = operations.findProperty("post")!!.value as JsonObject
        assertEquals("createItem", (post.findProperty("operationId")?.value as? JsonStringLiteral)?.value)
        assertEquals("Create an item", (post.findProperty("summary")?.value as? JsonStringLiteral)?.value)
        assertEquals(
            "The plugin must read the written spec as documenting GET and POST /items:\n$text",
            setOf("get", "post"),
            EndpointUsageSearcher.findOpenApiJsonEndpoints("/items", listOf("GET", "POST"), module)
                .map { (it as JsonProperty).name }.toSet()
        )
    }

    fun testYamlActionAddsAnUndocumentedPathAsANewPathItem() {
        val spec = addSpec("openapi.yaml", YAML_OTHER)
        val endpoint = itemsEndpoint()
        assertTrue("Precondition: GET /items is not documented", AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null))

        val text = applyActionFor(endpoint, spec)

        assertEquals("The documented /other must stay:\n$text", 1, yamlPathItems(text, "/other").size)
        val items = yamlPathItems(text, "/items")
        assertEquals("GET /items must be added as a new path item:\n$text", 1, items.size)
        assertEquals(setOf("get"), (items.single().value as YAMLMapping).keyValues.mapNotNull { it.keyText }.toSet())
    }

    fun testYamlActionIsNotOfferedAgainAfterAddingTheOperation() {
        val spec = addSpec("openapi.yaml", YAML_POST_ITEMS)
        assertPostItemsIsDocumentedAndGetIsMissing()
        val endpoint = itemsEndpoint()

        val text = applyActionFor(endpoint, spec)

        assertFalse(
            "GET /items is documented now, the action must not be offered again:\n$text",
            AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null)
        )
    }

    fun testJsonActionIsNotOfferedAgainAfterAddingTheOperation() {
        val spec = addSpec("openapi.json", JSON_POST_ITEMS)
        assertPostItemsIsDocumentedAndGetIsMissing()
        val endpoint = itemsEndpoint()

        val text = applyActionFor(endpoint, spec)

        assertFalse(
            "GET /items is documented now, the action must not be offered again:\n$text",
            AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null)
        )
    }

    private fun addSpec(name: String, text: String): VirtualFile {
        val spec = addFileToModule(module, name, text).virtualFile
        assertEquals(
            "Precondition: $name is the only OpenAPI spec of the project",
            listOf(spec),
            (EndpointUsageSearcher.findOpenApiJsonData(project) + EndpointUsageSearcher.findOpenApiYamlData(project))
                .map { it.psiFile.virtualFile }
        )
        return spec
    }

    private fun assertPostItemsIsDocumentedAndGetIsMissing() {
        val documented = EndpointUsageSearcher.findOpenApiYamlEndpoints("/items", listOf("POST"), module) +
                EndpointUsageSearcher.findOpenApiJsonEndpoints("/items", listOf("POST"), module)
        assertEquals("Precondition: the spec documents POST /items", 1, documented.size)
        val getOperations = EndpointUsageSearcher.findOpenApiYamlEndpoints("/items", listOf("GET"), module) +
                EndpointUsageSearcher.findOpenApiJsonEndpoints("/items", listOf("GET"), module)
        assertEquals("Precondition: the spec does not document GET /items", 0, getOperations.size)
    }

    private fun applyActionFor(endpoint: AddEndpointToOpenApiIntention.EndpointInfo, spec: VirtualFile): String {
        val intention = AddEndpointToOpenApiIntention(endpoint)
        assertTrue("Precondition: the action is offered for GET /items", intention.isAvailable(project, null, null))
        val original = PsiManager.getInstance(project).findFile(spec)!!.text

        intention.invoke(project, null, null)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val specFile = PsiManager.getInstance(project).findFile(spec)!!
        val text = specFile.text
        assertFalse("Precondition: the action wrote into the spec:\n$text", text == original)
        return text
    }

    private fun yamlPathItems(text: String, path: String): List<YAMLKeyValue> {
        val file = reparse("check.yaml", YAMLLanguage.INSTANCE, text) as YAMLFile
        assertFalse("The written spec must stay valid YAML:\n$text", PsiTreeUtil.hasErrorElements(file))
        val paths = (file.documents.single().topLevelValue as YAMLMapping).getKeyValueByKey("paths")!!.value as YAMLMapping
        return paths.keyValues.filter { it.keyText == path }
    }

    private fun jsonPathItems(text: String, path: String): List<JsonObject> {
        val file = reparse("check.json", JsonLanguage.INSTANCE, text) as JsonFile
        assertFalse("The written spec must stay valid JSON:\n$text", PsiTreeUtil.hasErrorElements(file))
        val paths = (file.topLevelValue as JsonObject).findProperty("paths")!!.value as JsonObject
        return paths.propertyList.filter { it.name == path }.map { it.value as JsonObject }
    }

    private fun reparse(name: String, language: Language, text: String): PsiFile =
        PsiFileFactory.getInstance(project).createFileFromText(name, language, text)

    private fun itemsEndpoint(): AddEndpointToOpenApiIntention.EndpointInfo {
        if (JavaPsiFacade.getInstance(project).findClass("com.example.ItemsController", GlobalSearchScope.projectScope(project)) == null) {
            addFileToModule(
                module, "com/example/ItemsController.java", """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class ItemsController {
                    @GetMapping("/items")
                    public String listItems() { return "items"; }
                }
                """.trimIndent()
            )
        }
        val method = JavaPsiFacade.getInstance(project)
            .findClass("com.example.ItemsController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("listItems", false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        val endpoint = (markers.single().navigationHandler as EndpointIconGutterHandler).endpointInfo
        assertEquals("/items", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
        return endpoint
    }
}

private val YAML_POST_ITEMS = """
    openapi: 3.0.1
    info:
      title: Items
      version: 1.0.0
    paths:
      /items:
        post:
          operationId: createItem
          summary: Create an item
""".trimIndent()

private val YAML_OTHER = """
    openapi: 3.0.1
    info:
      title: Items
      version: 1.0.0
    paths:
      /other:
        get:
          operationId: other
""".trimIndent()

private val JSON_POST_ITEMS = """
    {
      "openapi": "3.0.3",
      "info": {
        "title": "Items",
        "version": "1.0.0"
      },
      "paths": {
        "/items": {
          "post": {
            "operationId": "createItem",
            "summary": "Create an item"
          }
        }
      }
    }
""".trimIndent()

