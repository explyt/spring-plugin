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
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.Language
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
import org.jetbrains.yaml.psi.YAMLSequence

class AddEndpointToOpenApiIntentionPathEdgeCasesTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7
    )

    fun testYamlActionCreatesPathsInASpecWithoutPaths() {
        val spec = addSpec("openapi.yaml", YAML_HEADER)
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = yamlPaths(text)
        assertNotNull("The action must create paths:\n$text", paths)
        assertEquals(listOf("/items"), paths!!.keyValues.map { it.keyText })
        assertEquals(setOf("get"), (paths.keyValues.single().value as YAMLMapping).keyValues.map { it.keyText }.toSet())
        assertEquals("Items", ((yamlFile(text).documents.single().topLevelValue as YAMLMapping)
            .getKeyValueByKey("info")!!.value as YAMLMapping).getKeyValueByKey("title")?.valueText)
    }

    fun testJsonActionCreatesPathsInASpecWithoutPaths() {
        val spec = addSpec("openapi.json", JSON_HEADER)
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = jsonPaths(text)
        assertNotNull("The action must create paths:\n$text", paths)
        assertEquals(listOf("/items"), paths!!.propertyList.map { it.name })
        assertEquals(setOf("get"), (paths.propertyList.single().value as JsonObject).propertyList.map { it.name }.toSet())
        assertEquals(1, (jsonFile(text).topLevelValue as JsonObject).propertyList.count { it.name == "paths" })
    }

    fun testYamlActionMergesIntoThePathWrittenWithATrailingSlash() {
        val spec = addSpec("openapi.yaml", yamlWithPaths("/items/:\n    post:\n      operationId: createItem"))
        assertEquals("Precondition: the spec path normalises to the mapped path", "/items", SpringWebUtil.simplifyUrl("/items/"))
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = yamlPaths(text)!!
        assertEquals("The documented /items/ must be extended, not duplicated:\n$text", listOf("/items/"), paths.keyValues.map { it.keyText })
        val operations = paths.keyValues.single().value as YAMLMapping
        assertEquals(setOf("get", "post"), operations.keyValues.map { it.keyText }.toSet())
        assertEquals("createItem", (operations.getKeyValueByKey("post")!!.value as YAMLMapping).getKeyValueByKey("operationId")?.valueText)
    }

    fun testJsonActionMergesIntoThePathWrittenWithATrailingSlash() {
        val spec = addSpec("openapi.json", jsonWithPaths("\"/items/\": { \"post\": { \"operationId\": \"createItem\" } }"))
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = jsonPaths(text)!!
        assertEquals("The documented /items/ must be extended, not duplicated:\n$text", listOf("/items/"), paths.propertyList.map { it.name })
        val operations = paths.propertyList.single().value as JsonObject
        assertEquals(setOf("get", "post"), operations.propertyList.map { it.name }.toSet())
        assertEquals("createItem", ((operations.findProperty("post")!!.value as JsonObject).findProperty("operationId")?.value as JsonStringLiteral).value)
    }

    fun testYamlActionKeepsTheOtherKeysOfThePathItem() {
        val spec = addSpec(
            "openapi.yaml", yamlWithPaths(
                """
                /items:
                    summary: Items collection
                    parameters:
                      - name: tenant
                        in: header
                        schema:
                          type: string
                    post:
                      operationId: createItem
                """.trimIndent()
            )
        )
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = yamlPaths(text)!!
        assertEquals(listOf("/items"), paths.keyValues.map { it.keyText })
        val item = paths.keyValues.single().value as YAMLMapping
        assertEquals(setOf("summary", "parameters", "post", "get"), item.keyValues.map { it.keyText }.toSet())
        assertEquals("Items collection", item.getKeyValueByKey("summary")?.valueText)
        val parameters = (item.getKeyValueByKey("parameters")!!.value as YAMLSequence).items
        assertEquals(1, parameters.size)
        val tenant = parameters.single().value as YAMLMapping
        assertEquals("tenant", tenant.getKeyValueByKey("name")?.valueText)
        assertEquals("header", tenant.getKeyValueByKey("in")?.valueText)
        assertEquals("createItem", (item.getKeyValueByKey("post")!!.value as YAMLMapping).getKeyValueByKey("operationId")?.valueText)
    }

    fun testJsonActionKeepsTheOtherKeysOfThePathItem() {
        val spec = addSpec(
            "openapi.json", jsonWithPaths(
                "\"/items\": { \"summary\": \"Items collection\", " +
                        "\"parameters\": [ { \"name\": \"tenant\", \"in\": \"header\", \"schema\": { \"type\": \"string\" } } ], " +
                        "\"post\": { \"operationId\": \"createItem\" } }"
            )
        )
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = jsonPaths(text)!!
        assertEquals(listOf("/items"), paths.propertyList.map { it.name })
        val item = paths.propertyList.single().value as JsonObject
        assertEquals(setOf("summary", "parameters", "post", "get"), item.propertyList.map { it.name }.toSet())
        assertEquals("Items collection", (item.findProperty("summary")!!.value as JsonStringLiteral).value)
        val parameters = (item.findProperty("parameters")!!.value as JsonArray).valueList
        assertEquals(1, parameters.size)
        val tenant = parameters.single() as JsonObject
        assertEquals("tenant", (tenant.findProperty("name")!!.value as JsonStringLiteral).value)
        assertEquals("header", (tenant.findProperty("in")!!.value as JsonStringLiteral).value)
        assertEquals("createItem", ((item.findProperty("post")!!.value as JsonObject).findProperty("operationId")?.value as JsonStringLiteral).value)
    }

    fun testYamlActionNeverReplacesADocumentedOperation() {
        val spec = addSpec("openapi.yaml", yamlWithPaths("/items:\n    post:\n      operationId: createItem\n      summary: Create an item"))
        val endpoint = endpointOf(GET_AND_POST_ITEMS)
        assertEquals("Precondition: the handler maps GET and POST", listOf("GET", "POST"), endpoint.requestMethods)

        val text = applyAction(endpoint, spec)

        val paths = yamlPaths(text)!!
        assertEquals(listOf("/items"), paths.keyValues.map { it.keyText })
        val operations = paths.keyValues.single().value as YAMLMapping
        assertEquals(setOf("get", "post"), operations.keyValues.map { it.keyText }.toSet())
        val post = operations.getKeyValueByKey("post")!!.value as YAMLMapping
        assertEquals(setOf("operationId", "summary"), post.keyValues.map { it.keyText }.toSet())
        assertEquals("createItem", post.getKeyValueByKey("operationId")?.valueText)
        assertEquals("Create an item", post.getKeyValueByKey("summary")?.valueText)
    }

    fun testJsonActionNeverReplacesADocumentedOperation() {
        val spec = addSpec("openapi.json", jsonWithPaths("\"/items\": { \"post\": { \"operationId\": \"createItem\", \"summary\": \"Create an item\" } }"))
        val endpoint = endpointOf(GET_AND_POST_ITEMS)
        assertEquals("Precondition: the handler maps GET and POST", listOf("GET", "POST"), endpoint.requestMethods)

        val text = applyAction(endpoint, spec)

        val paths = jsonPaths(text)!!
        assertEquals(listOf("/items"), paths.propertyList.map { it.name })
        val operations = paths.propertyList.single().value as JsonObject
        assertEquals(setOf("post", "get"), operations.propertyList.map { it.name }.toSet())
        assertEquals(2, operations.propertyList.size)
        val post = operations.findProperty("post")!!.value as JsonObject
        assertEquals(listOf("operationId", "summary"), post.propertyList.map { it.name })
        assertEquals("Create an item", (post.findProperty("summary")!!.value as JsonStringLiteral).value)
    }

    fun testYamlActionFillsAPathItemWithoutOperations() {
        val spec = addSpec("openapi.yaml", yamlWithPaths("/items:\n/other:\n  get:\n    operationId: other"))
        assertEquals(
            "Precondition: /items has no operations and /other documents GET",
            listOf("/items", "/other"), yamlPaths(PsiManager.getInstance(project).findFile(spec)!!.text)!!.keyValues.map { it.keyText }
        )
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = yamlPaths(text)!!
        assertEquals("One /items and the untouched /other:\n$text", listOf("/items", "/other").sorted(), paths.keyValues.map { it.keyText }.sorted())
        assertEquals(setOf("get"), (paths.getKeyValueByKey("/items")!!.value as YAMLMapping).keyValues.map { it.keyText }.toSet())
        assertEquals("other", ((paths.getKeyValueByKey("/other")!!.value as YAMLMapping).getKeyValueByKey("get")!!.value as YAMLMapping)
            .getKeyValueByKey("operationId")?.valueText)
    }

    fun testJsonActionAddsAnUndocumentedPathAsANewPathItem() {
        val spec = addSpec("openapi.json", jsonWithPaths("\"/other\": { \"get\": { \"operationId\": \"other\" } }"))
        val endpoint = endpointOf(GET_ITEMS)
        assertTrue("Precondition: GET /items is not documented", isOffered(endpoint))

        val text = applyAction(endpoint, spec)

        val paths = jsonPaths(text)!!
        assertEquals(setOf("/other", "/items"), paths.propertyList.map { it.name }.toSet())
        assertEquals(2, paths.propertyList.size)
        assertEquals(setOf("get"), (paths.findProperty("/items")!!.value as JsonObject).propertyList.map { it.name }.toSet())
    }

    fun testYamlActionIsNotOfferedWhenThePathDocumentsEveryMappedMethod() {
        addSpec("openapi.yaml", yamlWithPaths("/items:\n    get:\n      operationId: listItems\n    post:\n      operationId: createItem"))
        val endpoint = endpointOf(GET_ITEMS)
        assertEquals(
            "Precondition: the spec documents GET and POST /items",
            setOf("get", "post"),
            EndpointUsageSearcher.findOpenApiYamlEndpoints("/items", listOf("GET", "POST"), module).map { (it as YAMLKeyValue).keyText }.toSet()
        )

        assertFalse(isOffered(endpoint))
    }

    fun testJsonActionIsNotOfferedWhenThePathDocumentsEveryMappedMethod() {
        addSpec("openapi.json", jsonWithPaths("\"/items\": { \"get\": { \"operationId\": \"listItems\" }, \"post\": { \"operationId\": \"createItem\" } }"))
        val endpoint = endpointOf(GET_ITEMS)
        assertEquals(
            "Precondition: the spec documents GET and POST /items",
            setOf("get", "post"),
            EndpointUsageSearcher.findOpenApiJsonEndpoints("/items", listOf("GET", "POST"), module).map { (it as JsonProperty).name }.toSet()
        )

        assertFalse(isOffered(endpoint))
    }

    private fun isOffered(endpoint: AddEndpointToOpenApiIntention.EndpointInfo) =
        AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null)

    private fun addSpec(name: String, text: String): VirtualFile {
        val spec = addFileToModule(module, name, text).virtualFile
        assertFalse("Precondition: $name parses:\n$text", PsiTreeUtil.hasErrorElements(PsiManager.getInstance(project).findFile(spec)!!))
        assertEquals(
            "Precondition: $name is the only OpenAPI spec of the project",
            listOf(spec),
            (EndpointUsageSearcher.findOpenApiJsonData(project) + EndpointUsageSearcher.findOpenApiYamlData(project))
                .map { it.psiFile.virtualFile }
        )
        return spec
    }

    private fun applyAction(endpoint: AddEndpointToOpenApiIntention.EndpointInfo, spec: VirtualFile): String {
        val original = PsiManager.getInstance(project).findFile(spec)!!.text

        AddEndpointToOpenApiIntention(endpoint).invoke(project, null, null)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val text = PsiManager.getInstance(project).findFile(spec)!!.text
        assertFalse("Precondition: the action wrote into the spec:\n$text", text == original)
        return text
    }

    private fun yamlFile(text: String): YAMLFile {
        val file = reparse("check.yaml", YAMLLanguage.INSTANCE, text) as YAMLFile
        assertFalse("The written spec must stay valid YAML:\n$text", PsiTreeUtil.hasErrorElements(file))
        return file
    }

    private fun yamlPaths(text: String): YAMLMapping? =
        (yamlFile(text).documents.single().topLevelValue as YAMLMapping).getKeyValueByKey("paths")?.value as? YAMLMapping

    private fun jsonFile(text: String): JsonFile {
        val file = reparse("check.json", JsonLanguage.INSTANCE, text) as JsonFile
        assertFalse("The written spec must stay valid JSON:\n$text", PsiTreeUtil.hasErrorElements(file))
        return file
    }

    private fun jsonPaths(text: String): JsonObject? =
        (jsonFile(text).topLevelValue as JsonObject).findProperty("paths")?.value as? JsonObject

    private fun reparse(name: String, language: Language, text: String): PsiFile =
        PsiFileFactory.getInstance(project).createFileFromText(name, language, text)

    private fun endpointOf(handler: String): AddEndpointToOpenApiIntention.EndpointInfo {
        addFileToModule(
            module, "com/example/ItemsController.java",
            "package com.example;\n\n" +
                    "import org.springframework.web.bind.annotation.*;\n\n" +
                    "@RestController\n" +
                    "public class ItemsController {\n" +
                    "    $handler\n" +
                    "}\n"
        )
        val method = JavaPsiFacade.getInstance(project)
            .findClass("com.example.ItemsController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("handle", false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        return (markers.single().navigationHandler as EndpointIconGutterHandler).endpointInfo
    }

    private fun yamlWithPaths(paths: String) = "$YAML_HEADER\npaths:\n  " + paths.replace("\n", "\n  ")

    private fun jsonWithPaths(paths: String) = JSON_HEADER.removeSuffix("}").trimEnd() + ",\n  \"paths\": { $paths }\n}"
}

private const val GET_ITEMS = "@GetMapping(\"/items\") public String handle() { return \"items\"; }"

private const val GET_AND_POST_ITEMS =
    "@RequestMapping(value = \"/items\", method = {RequestMethod.GET, RequestMethod.POST}) public String handle() { return \"items\"; }"

private val YAML_HEADER = """
    openapi: 3.0.1
    info:
      title: Items
      version: 1.0.0
""".trimIndent()

private val JSON_HEADER = """
    {
      "openapi": "3.0.3",
      "info": {
        "title": "Items",
        "version": "1.0.0"
      }
    }
""".trimIndent()
