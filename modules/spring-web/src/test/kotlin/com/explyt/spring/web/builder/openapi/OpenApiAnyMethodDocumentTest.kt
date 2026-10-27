/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.providers.EndpointRunLineMarkerProvider
import com.explyt.spring.web.providers.RunInSwaggerAction
import com.explyt.spring.web.util.OpenApiFileUtil
import com.explyt.spring.web.util.SpringWebUtil
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.json.JsonFileType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

class OpenApiAnyMethodDocumentTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
    )

    fun testGeneratedDocumentGivesAJavaRouteThatAnswersEveryMethodSevenOperations() {
        addJavaController()

        val endpoint = projectEndpointInfo("com.example.JavaAnyController", "any")

        assertEquals(mapOf("/x" to SPRINGDOC_OPERATIONS), yamlDocumentOperations(endpoint))
        assertEquals(mapOf("/x" to SPRINGDOC_OPERATIONS), jsonDocumentOperations(endpoint))
    }

    fun testGeneratedDocumentGivesAKotlinRouteThatAnswersEveryMethodSevenOperations() {
        addKotlinController()

        val endpoint = projectEndpointInfo("com.example.KotlinAnyController", "any")

        assertEquals(mapOf("/x" to SPRINGDOC_OPERATIONS), yamlDocumentOperations(endpoint))
        assertEquals(mapOf("/x" to SPRINGDOC_OPERATIONS), jsonDocumentOperations(endpoint))
    }

    fun testGeneratedDocumentKeepsTheListedOperationOfAGetMapping() {
        addJavaController()

        val endpoint = endpointInfoOf("com.example.JavaAnyController", "y")
        assertEquals(listOf("GET"), endpoint.requestMethods)

        assertEquals(mapOf("/y" to listOf("get")), yamlDocumentOperations(endpoint))
        assertEquals(mapOf("/y" to listOf("get")), jsonDocumentOperations(endpoint))
    }

    fun testRunInSwaggerOpensOneDocumentWithSevenOperationsForAJavaRoute() {
        addJavaController()
        val method = methodOf("com.example.JavaAnyController", "any")

        val endpoint = runInSwaggerEndpoint(method.nameIdentifier!!)

        assertEquals(mapOf("/x" to SPRINGDOC_OPERATIONS), yamlDocumentOperations(endpoint))
    }

    fun testRunInSwaggerOpensOneDocumentWithSevenOperationsForAKotlinRoute() {
        addKotlinController()
        val method = methodOf("com.example.KotlinAnyController", "any")

        val endpoint = runInSwaggerEndpoint((method.navigationElement as KtNamedFunction).nameIdentifier!!)

        assertEquals(mapOf("/x" to SPRINGDOC_OPERATIONS), yamlDocumentOperations(endpoint))
    }

    fun testRunInSwaggerKeepsTheListedOperationOfAGetMapping() {
        addJavaController()
        val method = methodOf("com.example.JavaAnyController", "y")

        val endpoint = runInSwaggerEndpoint(method.nameIdentifier!!, expectedMethods = listOf("GET"), expectedPath = "/y")

        assertEquals(mapOf("/y" to listOf("get")), yamlDocumentOperations(endpoint))
    }

    fun testYamlDocumentGivesEachVerbOnceWhenAJavaAnyMethodRouteSharesItsPathWithAGetMapping() {
        addJavaSharedPathController()

        val operations = yamlOperations(sharedPathEndpoints("com.example.JavaSharedPathController"))

        assertEquals(setOf("/x"), operations.keys)
        assertEachVerbOnceWithGetFromTheListedHandler(operations.getValue("/x"))
    }

    fun testJsonDocumentGivesEachVerbOnceWhenAJavaAnyMethodRouteSharesItsPathWithAGetMapping() {
        addJavaSharedPathController()

        val operations = strictJsonOperations(sharedPathEndpoints("com.example.JavaSharedPathController"))

        assertEquals(setOf("/x"), operations.keys)
        assertEachVerbOnceWithGetFromTheListedHandler(operations.getValue("/x"))
    }

    fun testYamlDocumentGivesEachVerbOnceWhenAKotlinAnyMethodRouteSharesItsPathWithAGetMapping() {
        addKotlinSharedPathController()

        val operations = yamlOperations(sharedPathEndpoints("com.example.KotlinSharedPathController"))

        assertEquals(setOf("/x"), operations.keys)
        assertEachVerbOnceWithGetFromTheListedHandler(operations.getValue("/x"))
    }

    fun testJsonDocumentGivesEachVerbOnceWhenAKotlinAnyMethodRouteSharesItsPathWithAGetMapping() {
        addKotlinSharedPathController()

        val operations = strictJsonOperations(sharedPathEndpoints("com.example.KotlinSharedPathController"))

        assertEquals(setOf("/x"), operations.keys)
        assertEachVerbOnceWithGetFromTheListedHandler(operations.getValue("/x"))
    }

    fun testDocumentKeepsOneOperationPerListedVerbWhenGetAndPostMappingsShareAPath() {
        addJavaGetAndPostController()
        val get = endpointInfoOf("com.example.JavaGetAndPostController", "read")
        val post = endpointInfoOf("com.example.JavaGetAndPostController", "write")
        assertEquals("/x", get.path)
        assertEquals("/x", post.path)
        assertEquals(listOf("GET"), get.requestMethods)
        assertEquals(listOf("POST"), post.requestMethods)

        val expected = mapOf("/x" to listOf("get" to "read", "post" to "write"))
        assertEquals(expected, yamlOperations(listOf(get, post)))
        assertEquals(expected, strictJsonOperations(listOf(get, post)))
    }

    private fun sharedPathEndpoints(className: String): List<EndpointInfo> {
        val any = endpointInfoOf(className, "any")
        val listed = endpointInfoOf(className, "listed")
        assertEquals("Precondition: the any-method handler path", "/x", any.path)
        assertEquals("Precondition: the listed handler path", "/x", listed.path)
        assertEquals("Precondition: the any-method handler declares no method", emptyList<String>(), any.requestMethods)
        assertEquals("Precondition: the listed handler declares GET", listOf("GET"), listed.requestMethods)
        return listOf(any, listed)
    }

    private fun assertEachVerbOnceWithGetFromTheListedHandler(operations: List<Pair<String, String?>>) {
        assertEquals(
            "Each springdoc verb must appear exactly once under /x: $operations",
            SPRINGDOC_OPERATIONS.associateWith { 1 },
            operations.groupingBy { it.first }.eachCount(),
        )
        val handlerByVerb = operations.toMap()
        assertEquals("GET dispatches to the @GetMapping handler", "listed", handlerByVerb["get"])
        SPRINGDOC_OPERATIONS.filter { it != "get" }.forEach { verb ->
            assertEquals("$verb belongs to the any-method handler", "any", handlerByVerb[verb])
        }
    }

    private fun yamlOperations(endpoints: List<EndpointInfo>): Map<String, List<Pair<String, String?>>> {
        val text = endpoints.fold(
            OpenApiBuilderFactory.getOpenApiFileBuilder(YAMLFileType.YML).addServer(OpenApiFileUtil.DEFAULT_SERVER)
        ) { builder, endpoint -> builder.addEndpoint(endpoint) }.toString()
        val file = PsiFileFactory.getInstance(project).createFileFromText("check.yaml", YAMLLanguage.INSTANCE, text) as YAMLFile
        assertFalse("The generated document must be valid YAML:\n$text", PsiTreeUtil.hasErrorElements(file))
        val paths = (file.documents.single().topLevelValue as YAMLMapping).getKeyValueByKey("paths")!!.value as YAMLMapping
        return paths.keyValues.associate { pathItem ->
            pathItem.keyText to (pathItem.value as? YAMLMapping)?.keyValues?.map { operation ->
                operation.keyText to (operation.value as? YAMLMapping)?.getKeyValueByKey("operationId")?.valueText
            }.orEmpty()
        }
    }

    private fun strictJsonOperations(endpoints: List<EndpointInfo>): Map<String, List<Pair<String, String?>>> {
        val text = endpoints.fold(
            OpenApiBuilderFactory.getOpenApiFileBuilder(JsonFileType.INSTANCE).addServer(OpenApiFileUtil.DEFAULT_SERVER)
        ) { builder, endpoint -> builder.addEndpoint(endpoint) }.toString()
        val paths = ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(text)["paths"]
        return paths.fieldNames().asSequence().associateWith { path ->
            paths[path].fieldNames().asSequence().map { verb -> verb to paths[path][verb]["operationId"]?.asText() }.toList()
        }
    }

    private fun projectEndpointInfo(className: String, methodName: String): EndpointInfo {
        val endpoint = endpointInfoOf(className, methodName)
        assertEquals("/x", endpoint.path)
        assertEquals("Precondition: the mapping declares no method", emptyList<String>(), endpoint.requestMethods)
        return endpoint
    }

    private fun endpointInfoOf(className: String, methodName: String): EndpointInfo {
        val method = methodOf(className, methodName).toUElement() as UMethod
        return SpringWebUtil.getEndpointInfo(method) ?: error("$className.$methodName is not an endpoint")
    }

    private fun runInSwaggerEndpoint(
        identifier: PsiElement,
        expectedMethods: List<String> = emptyList(),
        expectedPath: String = "/x",
    ): EndpointInfo {
        val action = EndpointRunLineMarkerProvider().getInfo(identifier)?.actions
            ?.filterIsInstance<RunInSwaggerAction>()?.single()
            ?: error("No Run in Swagger action on ${identifier.text}")
        val endpoint = action.endpoints().single()
        assertEquals(expectedPath, endpoint.path)
        assertEquals("Precondition: the declared methods of ${identifier.text}", expectedMethods, endpoint.requestMethods)
        return endpoint
    }

    private fun yamlDocumentOperations(endpoint: EndpointInfo): Map<String, List<String>> {
        val text = OpenApiBuilderFactory.getOpenApiFileBuilder(YAMLFileType.YML)
            .addServer(OpenApiFileUtil.DEFAULT_SERVER)
            .addEndpoint(endpoint)
            .toString()
        val file = PsiFileFactory.getInstance(project).createFileFromText("check.yaml", YAMLLanguage.INSTANCE, text) as YAMLFile
        assertFalse("The generated document must be valid YAML:\n$text", PsiTreeUtil.hasErrorElements(file))
        val paths = (file.documents.single().topLevelValue as YAMLMapping).getKeyValueByKey("paths")!!.value as YAMLMapping
        return paths.keyValues.associate { pathItem ->
            pathItem.keyText to (pathItem.value as? YAMLMapping)?.keyValues?.mapNotNull { it.keyText }.orEmpty()
        }
    }

    private fun jsonDocumentOperations(endpoint: EndpointInfo): Map<String, List<String>> {
        val text = OpenApiBuilderFactory.getOpenApiFileBuilder(JsonFileType.INSTANCE)
            .addServer(OpenApiFileUtil.DEFAULT_SERVER)
            .addEndpoint(endpoint)
            .toString()
        val paths = ObjectMapper().readTree(text)["paths"]
        return paths.fieldNames().asSequence().associateWith { paths[it].fieldNames().asSequence().toList() }
    }

    private fun methodOf(className: String, methodName: String): PsiMethod =
        JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
            ?.findMethodsByName(methodName, false)?.single()
            ?: error("$className.$methodName is not in the fixture")

    private fun addJavaController() {
        myFixture.addFileToProject(
            "com/example/JavaAnyController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class JavaAnyController {
                @RequestMapping("/x")
                public String any() { return "any"; }

                @GetMapping("/y")
                public String y() { return "y"; }
            }
            """.trimIndent()
        )
    }

    private fun addJavaSharedPathController() {
        myFixture.addFileToProject(
            "com/example/JavaSharedPathController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class JavaSharedPathController {
                @RequestMapping("/x")
                public String any() { return "any"; }

                @GetMapping("/x")
                public String listed() { return "listed"; }
            }
            """.trimIndent()
        )
    }

    private fun addKotlinSharedPathController() {
        myFixture.addFileToProject(
            "com/example/KotlinSharedPathController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class KotlinSharedPathController {
                @RequestMapping("/x")
                fun any(): String = "any"

                @GetMapping("/x")
                fun listed(): String = "listed"
            }
            """.trimIndent()
        )
    }

    private fun addJavaGetAndPostController() {
        myFixture.addFileToProject(
            "com/example/JavaGetAndPostController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class JavaGetAndPostController {
                @GetMapping("/x")
                public String read() { return "read"; }

                @PostMapping("/x")
                public String write() { return "write"; }
            }
            """.trimIndent()
        )
    }

    private fun addKotlinController() {
        myFixture.addFileToProject(
            "com/example/KotlinAnyController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class KotlinAnyController {
                @RequestMapping("/x")
                fun any(): String = "any"
            }
            """.trimIndent()
        )
    }
}

private val SPRINGDOC_OPERATIONS = listOf("get", "post", "put", "patch", "delete", "options", "head")
