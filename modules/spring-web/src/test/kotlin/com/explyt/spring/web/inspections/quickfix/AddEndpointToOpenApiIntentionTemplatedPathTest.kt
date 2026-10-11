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
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class AddEndpointToOpenApiIntentionTemplatedPathTest : ExplytMultiModuleTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springWeb_6_0_7)

    fun testYamlActionMergesDifferentlyNamedTemplateAndRenamesParameter() {
        val spec = addSpec("openapi.yaml", YAML_GET_ITEM)
        val endpoint = deleteItemEndpoint()
        assertEquals("Precondition: GET is documented", 1, EndpointUsageSearcher.findOpenApiYamlEndpoints("/items/{itemId}", listOf("GET"), module).size)
        assertTrue("Precondition: DELETE is offered", isOffered(endpoint))
        val text = apply(endpoint, spec)
        val paths = (PsiFileFactory.getInstance(project).createFileFromText("x.yaml", YAMLLanguage.INSTANCE, text) as YAMLFile)
            .documents.single().topLevelValue as YAMLMapping
        val pathItems = paths.getKeyValueByKey("paths")!!.value as YAMLMapping
        assertEquals(1, pathItems.keyValues.size)
        val operations = pathItems.keyValues.single().value as YAMLMapping
        assertEquals(setOf("get", "delete"), operations.keyValues.map { it.keyText }.toSet())
        val parameters = (operations.getKeyValueByKey("delete")!!.value as YAMLMapping).getKeyValueByKey("parameters")!!.value as org.jetbrains.yaml.psi.YAMLSequence
        assertEquals("itemId", (parameters.items.single().value as YAMLMapping).getKeyValueByKey("name")?.valueText)
    }

    fun testJsonActionMergesDifferentlyNamedTemplateAndRenamesParameter() {
        val spec = addSpec("openapi.json", JSON_GET_ITEM)
        val endpoint = deleteItemEndpoint()
        assertEquals("Precondition: GET is documented", 1, EndpointUsageSearcher.findOpenApiJsonEndpoints("/items/{itemId}", listOf("GET"), module).size)
        assertTrue("Precondition: DELETE is offered", isOffered(endpoint))
        val text = apply(endpoint, spec)
        val root = (PsiFileFactory.getInstance(project).createFileFromText("x.json", JsonLanguage.INSTANCE, text) as JsonFile).topLevelValue as JsonObject
        val pathItems = root.findProperty("paths")!!.value as JsonObject
        assertEquals(1, pathItems.propertyList.size)
        val operations = pathItems.propertyList.single().value as JsonObject
        assertEquals(setOf("get", "delete"), operations.propertyList.map { it.name }.toSet())
        val parameters = (operations.findProperty("delete")!!.value as JsonObject).findProperty("parameters")!!.value as JsonArray
        assertEquals("itemId", ((parameters.valueList.single() as JsonObject).findProperty("name")!!.value as JsonStringLiteral).value)
    }

    fun testGetActionIsNotOfferedForSameTemplatedPathWithAnotherVariableName() {
        addSpec("openapi.yaml", YAML_GET_ITEM)
        val endpoint = getItemEndpoint()
        assertFalse("GET under the same templated path is already documented", isOffered(endpoint))
    }

    private fun isOffered(endpoint: AddEndpointToOpenApiIntention.EndpointInfo) = AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null)

    private fun addSpec(name: String, text: String): VirtualFile {
        val file = addFileToModule(module, name, text).virtualFile
        assertEquals(listOf(file), (EndpointUsageSearcher.findOpenApiJsonData(project) + EndpointUsageSearcher.findOpenApiYamlData(project)).map { it.psiFile.virtualFile })
        return file
    }

    private fun apply(endpoint: AddEndpointToOpenApiIntention.EndpointInfo, spec: VirtualFile): String {
        AddEndpointToOpenApiIntention(endpoint).invoke(project, null, null)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val text = PsiManager.getInstance(project).findFile(spec)!!.text
        assertFalse("The written spec must remain parseable", PsiTreeUtil.hasErrorElements(PsiManager.getInstance(project).findFile(spec)!!))
        return text
    }

    private fun deleteItemEndpoint() = endpoint("deleteItem", "@DeleteMapping(\"/items/{id}\") public void deleteItem(@PathVariable String id) { }")
    private fun getItemEndpoint() = endpoint("getItem", "@GetMapping(\"/items/{id}\") public void getItem(String id) { }")

    private fun endpoint(name: String, method: String): AddEndpointToOpenApiIntention.EndpointInfo {
        addFileToModule(module, "com/example/ItemsController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.*;
            @RestController public class ItemsController { $method }
        """.trimIndent())
        val psiMethod = JavaPsiFacade.getInstance(project).findClass("com.example.ItemsController", GlobalSearchScope.projectScope(project))!!.findMethodsByName(name, false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(psiMethod.nameIdentifier!!), markers)
        return (markers.single().navigationHandler as EndpointIconGutterHandler).endpointInfo
    }
}

private val YAML_GET_ITEM = """
openapi: 3.0.1
info:
  title: Items
  version: 1.0.0
paths:
  /items/{itemId}:
    get:
      operationId: getItem
""".trimIndent()

private val JSON_GET_ITEM = """
{"openapi":"3.0.3","info":{"title":"Items","version":"1.0.0"},"paths":{"/items/{itemId}":{"get":{"operationId":"getItem"}}}}
""".trimIndent()
