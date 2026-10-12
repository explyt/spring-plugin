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

class AddEndpointToOpenApiIntentionAnyMethodTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7
    )

    fun testYamlActionDocumentsEveryOperationOfARouteThatAnswersEveryMethod() {
        val spec = addSpec("openapi.yaml", yamlSpec("/other" to listOf("get")))

        val text = applyActionFor(anyMethodEndpoint(), spec)

        assertEquals("The route answers every method, like springdoc it gets seven operations:\n$text",
            SPRINGDOC_OPERATIONS, yamlOperations(text, "/x"))
        assertEquals("The documented /other must stay:\n$text", listOf("get"), yamlOperations(text, "/other"))
    }

    fun testJsonActionDocumentsEveryOperationOfARouteThatAnswersEveryMethod() {
        val spec = addSpec("openapi.json", jsonSpec("/other" to listOf("get")))

        val text = applyActionFor(anyMethodEndpoint(), spec)

        assertEquals("The route answers every method, like springdoc it gets seven operations:\n$text",
            SPRINGDOC_OPERATIONS, jsonOperations(text, "/x").map { it.name })
        assertEquals("The documented /other must stay:\n$text", listOf("get"), jsonOperations(text, "/other").map { it.name })
    }

    fun testYamlActionAddsOnlyTheUndocumentedOperations() {
        val spec = addSpec("openapi.yaml", yamlSpec("/x" to listOf("get"), "/other" to listOf("get")))
        assertEquals(
            "Precondition: the spec documents GET /x",
            1, EndpointUsageSearcher.findOpenApiYamlEndpoints("/x", listOf("GET"), module).size
        )

        val text = applyActionFor(anyMethodEndpoint(), spec)

        val operations = yamlPathItem(text, "/x")
        assertEquals(
            "The six missing operations must be added next to the documented GET:\n$text",
            SPRINGDOC_OPERATIONS.toSet(), operations.keyValues.mapNotNull { it.keyText }.toSet()
        )
        val get = operations.getKeyValueByKey("get")!!.value as YAMLMapping
        assertEquals("The documented GET must be kept:\n$text", "documented_get", get.getKeyValueByKey("operationId")?.valueText)
        assertEquals("The documented /other must stay:\n$text", listOf("get"), yamlOperations(text, "/other"))
    }

    fun testJsonActionAddsOnlyTheUndocumentedOperations() {
        val spec = addSpec("openapi.json", jsonSpec("/x" to listOf("get"), "/other" to listOf("get")))
        assertEquals(
            "Precondition: the spec documents GET /x",
            1, EndpointUsageSearcher.findOpenApiJsonEndpoints("/x", listOf("GET"), module).size
        )

        val text = applyActionFor(anyMethodEndpoint(), spec)

        val operations = jsonOperations(text, "/x")
        assertEquals(
            "The six missing operations must be added next to the documented GET:\n$text",
            SPRINGDOC_OPERATIONS.toSet(), operations.map { it.name }.toSet()
        )
        val get = operations.single { it.name == "get" }.value as JsonObject
        assertEquals(
            "The documented GET must be kept:\n$text",
            "documented_get", (get.findProperty("operationId")?.value as? JsonStringLiteral)?.value
        )
        assertEquals("The documented /other must stay:\n$text", listOf("get"), jsonOperations(text, "/other").map { it.name })
    }

    fun testYamlActionIsNotOfferedWhenEveryOperationIsDocumented() {
        addSpec("openapi.yaml", yamlSpec("/x" to SPRINGDOC_OPERATIONS))
        val endpoint = anyMethodEndpoint()
        assertEquals(
            "Precondition: the spec documents the seven operations of /x",
            SPRINGDOC_OPERATIONS.size,
            EndpointUsageSearcher.findOpenApiYamlEndpoints("/x", SPRINGDOC_OPERATIONS.map { it.uppercase() }, module).size
        )

        assertFalse(AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null))
    }

    fun testJsonActionIsNotOfferedWhenEveryOperationIsDocumented() {
        addSpec("openapi.json", jsonSpec("/x" to SPRINGDOC_OPERATIONS))
        val endpoint = anyMethodEndpoint()
        assertEquals(
            "Precondition: the spec documents the seven operations of /x",
            SPRINGDOC_OPERATIONS.size,
            EndpointUsageSearcher.findOpenApiJsonEndpoints("/x", SPRINGDOC_OPERATIONS.map { it.uppercase() }, module).size
        )

        assertFalse(AddEndpointToOpenApiIntention(endpoint).isAvailable(project, null, null))
    }

    fun testYamlActionWritesOnlyTheListedOperationOfAGetMapping() {
        val spec = addSpec("openapi.yaml", yamlSpec("/other" to listOf("get")))

        val text = applyActionFor(getEndpoint(), spec)

        assertEquals("A listed verb never expands:\n$text", listOf("get"), yamlOperations(text, "/y"))
    }

    fun testJsonActionWritesOnlyTheListedOperationOfAGetMapping() {
        val spec = addSpec("openapi.json", jsonSpec("/other" to listOf("get")))

        val text = applyActionFor(getEndpoint(), spec)

        assertEquals("A listed verb never expands:\n$text", listOf("get"), jsonOperations(text, "/y").map { it.name })
    }

    fun testJsonAndYamlSpecsAreFoundAsReferencesOfARouteThatAnswersEveryMethod() {
        addFileToModule(module, "openapi.yaml", yamlSpec("/x" to listOf("get", "post")))
        addFileToModule(module, "openapi.json", jsonSpec("/x" to listOf("get", "post")))
        val endpoint = anyMethodEndpoint()
        assertEquals(
            "Precondition: the YAML spec documents GET and POST /x",
            2, EndpointUsageSearcher.findOpenApiYamlEndpoints("/x", listOf("GET", "POST"), module).size
        )
        assertEquals(
            "Precondition: the JSON spec documents GET and POST /x",
            2, EndpointUsageSearcher.findOpenApiJsonEndpoints("/x", listOf("GET", "POST"), module).size
        )

        assertEquals(
            "The YAML operations of /x are references of the route that answers every method",
            setOf("get", "post"),
            EndpointUsageSearcher.findOpenApiYamlEndpoints(endpoint.path, endpoint.requestMethods, module)
                .map { (it as YAMLKeyValue).keyText }.toSet()
        )
        assertEquals(
            "The JSON operations of /x are references of the route that answers every method",
            setOf("get", "post"),
            EndpointUsageSearcher.findOpenApiJsonEndpoints(endpoint.path, endpoint.requestMethods, module)
                .map { (it as JsonProperty).name }.toSet()
        )
    }

    fun testYamlActionLeavesTheSiblingHandlersVerbToThatHandler() {
        val spec = addSpec("openapi.yaml", yamlSpec("/other" to listOf("get")))
        val (any, _) = sharedPathEndpoints()

        val text = applyActionFor(any, spec)

        assertEquals(
            "GET /x is dispatched to listed, so any documents only the other six verbs:\n$text",
            SIBLING_FREE_OPERATIONS.map { it to "any" }, yamlOperationIds(text, "/x")
        )
    }

    fun testJsonActionLeavesTheSiblingHandlersVerbToThatHandler() {
        val spec = addSpec("openapi.json", jsonSpec("/other" to listOf("get")))
        val (any, _) = sharedPathEndpoints()

        val text = applyActionFor(any, spec)

        assertEquals(
            "GET /x is dispatched to listed, so any documents only the other six verbs:\n$text",
            SIBLING_FREE_OPERATIONS.map { it to "any" }, jsonOperationIds(text, "/x")
        )
    }

    fun testYamlActionForTheSiblingHandlerStillDocumentsItsVerbAfterTheAnyMethodHandler() {
        val spec = addSpec("openapi.yaml", yamlSpec("/other" to listOf("get")))
        val (any, listed) = sharedPathEndpoints()
        applyActionFor(any, spec)

        val text = applyActionFor(listed, spec)

        assertEquals(
            "Each verb of /x is documented once, GET by listed and the rest by any:\n$text",
            SHARED_PATH_OPERATION_IDS, yamlOperationIds(text, "/x").sortedBy { SPRINGDOC_OPERATIONS.indexOf(it.first) }
        )
    }

    fun testJsonActionForTheSiblingHandlerStillDocumentsItsVerbAfterTheAnyMethodHandler() {
        val spec = addSpec("openapi.json", jsonSpec("/other" to listOf("get")))
        val (any, listed) = sharedPathEndpoints()
        applyActionFor(any, spec)

        val text = applyActionFor(listed, spec)

        assertEquals(
            "Each verb of /x is documented once, GET by listed and the rest by any:\n$text",
            SHARED_PATH_OPERATION_IDS, jsonOperationIds(text, "/x").sortedBy { SPRINGDOC_OPERATIONS.indexOf(it.first) }
        )
    }

    fun testYamlActionIsNotOfferedWhenOnlyTheSiblingHandlersVerbIsUndocumented() {
        addSpec("openapi.yaml", yamlSpec("/x" to SIBLING_FREE_OPERATIONS))
        val (any, _) = sharedPathEndpoints()
        assertEquals(
            "Precondition: the spec documents the six verbs of /x other than GET",
            SIBLING_FREE_OPERATIONS.size,
            EndpointUsageSearcher.findOpenApiYamlEndpoints("/x", SIBLING_FREE_OPERATIONS.map { it.uppercase() }, module).size
        )

        assertFalse(
            "The undocumented GET /x belongs to listed",
            AddEndpointToOpenApiIntention(any).isAvailable(project, null, null)
        )
    }

    fun testJsonActionIsNotOfferedWhenOnlyTheSiblingHandlersVerbIsUndocumented() {
        addSpec("openapi.json", jsonSpec("/x" to SIBLING_FREE_OPERATIONS))
        val (any, _) = sharedPathEndpoints()
        assertEquals(
            "Precondition: the spec documents the six verbs of /x other than GET",
            SIBLING_FREE_OPERATIONS.size,
            EndpointUsageSearcher.findOpenApiJsonEndpoints("/x", SIBLING_FREE_OPERATIONS.map { it.uppercase() }, module).size
        )

        assertFalse(
            "The undocumented GET /x belongs to listed",
            AddEndpointToOpenApiIntention(any).isAvailable(project, null, null)
        )
    }

    private fun sharedPathEndpoints(): Pair<AddEndpointToOpenApiIntention.EndpointInfo, AddEndpointToOpenApiIntention.EndpointInfo> {
        addFileToModule(
            module, "com/example/SharedPathController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class SharedPathController {
                @RequestMapping("/x")
                public String any() { return "any"; }

                @GetMapping("/x")
                public String listed() { return "listed"; }
            }
            """.trimIndent()
        )
        val any = endpointOf("com.example.SharedPathController", "any")
        val listed = endpointOf("com.example.SharedPathController", "listed")
        assertEquals("Precondition: any is mapped to /x", "/x", any.path)
        assertEquals("Precondition: any declares no method", emptyList<String>(), any.requestMethods)
        assertEquals("Precondition: listed is mapped to /x", "/x", listed.path)
        assertEquals("Precondition: listed declares GET", listOf("GET"), listed.requestMethods)
        return any to listed
    }

    private fun yamlOperationIds(text: String, path: String): List<Pair<String, String?>> =
        yamlPathItem(text, path).keyValues.map {
            it.keyText to (it.value as? YAMLMapping)?.getKeyValueByKey("operationId")?.valueText
        }

    private fun jsonOperationIds(text: String, path: String): List<Pair<String, String?>> =
        jsonOperations(text, path).map {
            it.name to ((it.value as? JsonObject)?.findProperty("operationId")?.value as? JsonStringLiteral)?.value
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

    private fun applyActionFor(endpoint: AddEndpointToOpenApiIntention.EndpointInfo, spec: VirtualFile): String {
        val intention = AddEndpointToOpenApiIntention(endpoint)
        assertTrue(
            "The action must be offered while an operation of ${endpoint.path} is undocumented",
            intention.isAvailable(project, null, null)
        )
        val original = PsiManager.getInstance(project).findFile(spec)!!.text

        intention.invoke(project, null, null)
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val text = PsiManager.getInstance(project).findFile(spec)!!.text
        assertFalse("The action must write into the spec:\n$text", text == original)
        return text
    }

    private fun yamlOperations(text: String, path: String): List<String> =
        yamlPathItem(text, path).keyValues.mapNotNull { it.keyText }

    private fun yamlPathItem(text: String, path: String): YAMLMapping {
        val file = reparse("check.yaml", YAMLLanguage.INSTANCE, text) as YAMLFile
        assertFalse("The written spec must stay valid YAML:\n$text", PsiTreeUtil.hasErrorElements(file))
        val paths = (file.documents.single().topLevelValue as YAMLMapping).getKeyValueByKey("paths")!!.value as YAMLMapping
        val items = paths.keyValues.filter { it.keyText == path }
        assertEquals("The spec must hold exactly one $path path item:\n$text", 1, items.size)
        return items.single().value as? YAMLMapping ?: error("$path has no operations:\n$text")
    }

    private fun jsonOperations(text: String, path: String): List<JsonProperty> {
        val file = reparse("check.json", JsonLanguage.INSTANCE, text) as JsonFile
        assertFalse("The written spec must stay valid JSON:\n$text", PsiTreeUtil.hasErrorElements(file))
        val paths = (file.topLevelValue as JsonObject).findProperty("paths")!!.value as JsonObject
        val items = paths.propertyList.filter { it.name == path }
        assertEquals("The spec must hold exactly one \"$path\" key:\n$text", 1, items.size)
        return (items.single().value as JsonObject).propertyList
    }

    private fun reparse(name: String, language: Language, text: String): PsiFile =
        PsiFileFactory.getInstance(project).createFileFromText(name, language, text)

    private fun anyMethodEndpoint(): AddEndpointToOpenApiIntention.EndpointInfo {
        val endpoint = endpointOf("any")
        assertEquals("/x", endpoint.path)
        assertEquals("Precondition: the mapping declares no method", emptyList<String>(), endpoint.requestMethods)
        return endpoint
    }

    private fun getEndpoint(): AddEndpointToOpenApiIntention.EndpointInfo {
        val endpoint = endpointOf("y")
        assertEquals("/y", endpoint.path)
        assertEquals(listOf("GET"), endpoint.requestMethods)
        return endpoint
    }

    private fun endpointOf(className: String, methodName: String): AddEndpointToOpenApiIntention.EndpointInfo {
        val method = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project))!!
            .findMethodsByName(methodName, false).single()
        val markers = mutableListOf<LineMarkerInfo<*>>()
        ControllerEndpointActionsLineMarkerProvider().collectSlowLineMarkers(mutableListOf(method.nameIdentifier!!), markers)
        return (markers.single().navigationHandler as EndpointIconGutterHandler).endpointInfo
    }

    private fun endpointOf(methodName: String): AddEndpointToOpenApiIntention.EndpointInfo {
        val scope = GlobalSearchScope.projectScope(project)
        if (JavaPsiFacade.getInstance(project).findClass("com.example.AnyController", scope) == null) {
            addFileToModule(
                module, "com/example/AnyController.java", """
                package com.example;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class AnyController {
                    @RequestMapping("/x")
                    public String any() { return "any"; }

                    @GetMapping("/y")
                    public String y() { return "y"; }
                }
                """.trimIndent()
            )
        }
        return endpointOf("com.example.AnyController", methodName)
    }
}

private val SPRINGDOC_OPERATIONS = listOf("get", "post", "put", "patch", "delete", "options", "head")

private val SIBLING_FREE_OPERATIONS = SPRINGDOC_OPERATIONS - "get"

private val SHARED_PATH_OPERATION_IDS =
    listOf("get" to "listed") + SIBLING_FREE_OPERATIONS.map { it to "any" }

private fun yamlSpec(vararg pathItems: Pair<String, List<String>>): String = buildString {
    appendLine("openapi: 3.0.1")
    appendLine("info:")
    appendLine("  title: Items")
    appendLine("  version: 1.0.0")
    appendLine("paths:")
    for ((path, verbs) in pathItems) {
        appendLine("  $path:")
        for (verb in verbs) {
            appendLine("    $verb:")
            appendLine("      operationId: documented_$verb")
        }
    }
}

private fun jsonSpec(vararg pathItems: Pair<String, List<String>>): String {
    val paths = pathItems.joinToString(",\n") { (path, verbs) ->
        val operations = verbs.joinToString(",\n") { verb ->
            "      \"$verb\": {\n        \"operationId\": \"documented_$verb\"\n      }"
        }
        "    \"$path\": {\n$operations\n    }"
    }
    return "{\n  \"openapi\": \"3.0.3\",\n  \"info\": {\n    \"title\": \"Items\",\n    \"version\": \"1.0.0\"\n  },\n" +
            "  \"paths\": {\n$paths\n  }\n}"
}
