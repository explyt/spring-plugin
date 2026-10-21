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
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
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

class AddEndpointToOpenApiIntentionFormatGuardTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7
    )

    fun testFlowStyleYamlPathItemIsNotChanged() {
        val original = """
            openapi: 3.0.1
            info:
              title: Items
              version: 1.0.0
            paths:
              /items: {post: {operationId: createItem, responses: {'200': {description: ok}}}}
        """.trimIndent()
        val spec = addSpec("openapi.yaml", original)
        val endpoint = endpoint()
        assertTrue("Precondition: GET /items is offered", offered(endpoint))

        val text = apply(endpoint, spec)

        assertEquals("Flow-style YAML must not be rewritten:\n$text", original, text)
        val file = parseYaml(text)
        val pathItem = ((file.documents.single().topLevelValue as YAMLMapping).getKeyValueByKey("paths")!!.value as YAMLMapping)
            .getKeyValueByKey("/items")!!.value as YAMLMapping
        assertEquals(
            "createItem",
            (pathItem.getKeyValueByKey("post")!!.value as YAMLMapping).getKeyValueByKey("operationId")?.valueText
        )
    }

    fun testFlowStyleEmptyYamlPathsIsNotChanged() {
        val original = """
            openapi: 3.0.1
            info:
              title: Items
              version: 1.0.0
            paths: {}
        """.trimIndent()
        val spec = addSpec("openapi.yaml", original)
        val endpoint = endpoint()
        assertTrue("Precondition: GET /items is offered", offered(endpoint))

        val text = apply(endpoint, spec)

        assertEquals("Flow-style paths must not be rewritten:\n$text", original, text)
        assertFalse("Flow-style paths must remain valid YAML", PsiTreeUtil.hasErrorElements(parseYaml(text)))
    }

    fun testJsonNullPathsIsNotChanged() {
        val original = """
            {"openapi":"3.0.1","info":{"title":"Items","version":"1.0.0"},"paths":null}
        """.trimIndent()
        val spec = addSpec("openapi.json", original)
        val endpoint = endpoint()
        assertTrue("Precondition: GET /items is offered", offered(endpoint))

        val text = apply(endpoint, spec)

        assertEquals("A null paths value must not be rewritten:\n$text", original, text)
        val root = parseJson(text).topLevelValue as JsonObject
        assertEquals(1, root.propertyList.count { it.name == "paths" })
    }

    fun testBlockYamlPathItemIsStillMerged() {
        val spec = addSpec("openapi.yaml", YAML_POST_ITEMS)
        val endpoint = endpoint()
        assertTrue("Precondition: GET /items is offered", offered(endpoint))

        val text = apply(endpoint, spec)
        val item = ((parseYaml(text).documents.single().topLevelValue as YAMLMapping).getKeyValueByKey("paths")!!.value as YAMLMapping)
            .getKeyValueByKey("/items")!!.value as YAMLMapping
        assertEquals(setOf("get", "post"), item.keyValues.map { it.keyText }.toSet())
    }

    fun testJsonObjectPathsIsStillMerged() {
        val spec = addSpec("openapi.json", JSON_POST_ITEMS)
        val endpoint = endpoint()
        assertTrue("Precondition: GET /items is offered", offered(endpoint))

        val text = apply(endpoint, spec)
        val item = (parseJson(text).topLevelValue as JsonObject).findProperty("paths")!!.value as JsonObject
        assertEquals(setOf("get", "post"), (item.findProperty("/items")!!.value as JsonObject).propertyList.map { it.name }.toSet())
    }

    private fun offered(endpoint: AddEndpointToOpenApiIntention.EndpointInfo) =
        AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null)

    private fun addSpec(name: String, text: String): VirtualFile {
        val file = addFileToModule(module, name, text).virtualFile
        assertFalse("Precondition: $name is valid", PsiTreeUtil.hasErrorElements(PsiManager.getInstance(project).findFile(file)!!))
        assertEquals(listOf(file), (EndpointUsageSearcher.findOpenApiJsonData(project) + EndpointUsageSearcher.findOpenApiYamlData(project)).map { it.psiFile.virtualFile })
        return file
    }

    private fun apply(endpoint: AddEndpointToOpenApiIntention.EndpointInfo, spec: VirtualFile): String {
        AddEndpointToOpenApiIntention(endpoint).invoke(project, null, null)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        return PsiManager.getInstance(project).findFile(spec)!!.text
    }

    private fun parseYaml(text: String) =
        PsiFileFactory.getInstance(project).createFileFromText("check.yaml", YAMLLanguage.INSTANCE, text) as YAMLFile

    private fun parseJson(text: String) =
        PsiFileFactory.getInstance(project).createFileFromText("check.json", JsonLanguage.INSTANCE, text) as JsonFile

    private fun endpoint(): AddEndpointToOpenApiIntention.EndpointInfo {
        addFileToModule(module, "com/example/ItemsController.java", """
            package com.example;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;
            @RestController public class ItemsController {
                @GetMapping("/items") public String handle() { return "items"; }
            }
        """.trimIndent())
        val method = JavaPsiFacade.getInstance(project).findClass("com.example.ItemsController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("handle", false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        return (markers.single().navigationHandler as EndpointIconGutterHandler).endpointInfo
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
""".trimIndent()

private val JSON_POST_ITEMS = """
    {"openapi":"3.0.3","info":{"title":"Items","version":"1.0.0"},"paths":{"/items":{"post":{"operationId":"createItem"}}}}
""".trimIndent()
