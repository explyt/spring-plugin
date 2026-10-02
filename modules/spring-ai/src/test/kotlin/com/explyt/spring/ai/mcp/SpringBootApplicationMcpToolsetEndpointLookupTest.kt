/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking

/**
 * How `explyt_find_spring_endpoint` and `explyt_get_spring_endpoint_contract` read a URL taken from a running
 * application, and what they state about its parameters and its declaration.
 */
class SpringBootApplicationMcpToolsetEndpointLookupTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    private fun projectPath(): String = project.basePath ?: ""

    private suspend fun find(url: String, httpMethod: String = ""): JsonNode =
        mapper.readTree(toolset.findEndpoint(urlPattern = url, projectPath = projectPath(), httpMethod = httpMethod))

    private fun paths(nodes: JsonNode): List<String> = nodes.map { it["fullPath"].asText() }

    /**
     * A URL copied from a browser or a log line resolves as the path the application serves: no route declares a
     * scheme, a host, a query or a fragment.
     */
    fun testDeployedUrlResolvesAsItsRequestPath() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = find("https://example.com:8443/api/demo/items/42?expand=true#details")

        assertEquals(listOf("/api/demo/items/{id}"), paths(root["endpoints"]))
        assertTrue("Nothing was dropped from the path, got $root", root["assumedPrefix"].isNull)
    }

    /**
     * A servlet context path is prepended by the deployment, usually from configuration the IDE cannot see, so
     * no route declares it. A URL carrying one used to answer "no such route" - an invitation to write a duplicate
     * of an endpoint that exists.
     */
    fun testUrlUnderAContextPathResolvesAndReportsThePrefix() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = find("https://example.com/t/api/demo/items/42?window=24h", httpMethod = "GET")

        assertEquals(listOf("/api/demo/items/{id}"), paths(root["endpoints"]))
        assertEquals("/t", root["assumedPrefix"].asText())
        assertEquals("getItem", root["endpoints"][0]["methodName"].asText())
    }

    fun testPrefixOfSeveralSegmentsIsDroppedWhole() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = find("/gateway/t/api/routes/export")

        assertEquals("/gateway/t", root["assumedPrefix"].asText())
        assertEquals(
            "The dispatching literal route still comes first",
            listOf("/api/routes/export", "/api/routes/{id}"),
            paths(root["endpoints"])
        )
    }

    /** A contract is looked up the same way, so a URL from a failing request can be inspected as it was sent. */
    fun testContractOfAUrlUnderAContextPath() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/t/api/demo/items/42", projectPath = projectPath(), httpMethod = "GET")
        )

        assertEquals("/t", root["assumedPrefix"].asText())
        assertEquals("COMPLETE", root["endpoints"].single()["contractStatus"].asText())
    }

    /**
     * A route that does not exist yet under a context path is still answered with its neighbourhood: every route
     * opens after the prefix, so read as written the URL shares nothing with any of them.
     */
    fun testMissUnderAContextPathListsTheNearestRoutes() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = find("https://example.com/t/api/routes/export/preview")

        assertEquals(0, root["totalCount"].asInt())
        assertEquals("/t", root["assumedPrefix"].asText())
        assertEquals("/api/routes/export", root["sharedPrefix"].asText())
        assertEquals(listOf("/api/routes/export", "/api/routes/{id}"), paths(root["nearestByPrefix"]))
    }

    /**
     * Dropping a prefix is a guess, and only an unambiguous match justifies it: the rest of the URL has to match a
     * route whole. `/api/demo` is a fragment of `/api/demo/items`, which the forgiving substring match accepts from a
     * caller who typed a fragment - but here the tool cut the fragment out itself, and a full URL naming no route is
     * answered with its neighbourhood instead.
     */
    fun testDroppedPrefixDoesNotTurnAFragmentIntoAMatch() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = find("/t/api/demo")

        assertEquals("'/api/demo' is only a fragment of the demo routes, got $root", 0, root["totalCount"].asInt())
        assertEquals("/t", root["assumedPrefix"].asText())
        assertEquals("/api/demo", root["sharedPrefix"].asText())
        assertTrue(
            "The routes under the fragment are the neighbourhood, got ${root["nearestByPrefix"]}",
            paths(root["nearestByPrefix"]).all { it.startsWith("/api/demo/") }
        )
    }

    /**
     * The shared path of a miss is spelled as the routes declare it. The URL carries a value where the route has a
     * `{template}`, and echoing the value would read as if a route with that literal segment existed.
     */
    fun testSharedPrefixOfAMissKeepsTheRouteTemplate() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = find("https://example.com/t/api/demo/items/42/history")

        assertEquals(0, root["totalCount"].asInt())
        assertEquals("/api/demo/items/{id}", root["sharedPrefix"].asText())
        assertEquals(listOf("/api/demo/items/{id}"), paths(root["nearestByPrefix"]))
    }

    /**
     * A route opening with a `{template}` would absorb whatever the dropped prefix left over, so under an assumed
     * prefix it proves nothing about which route was requested: `/t/acme/reports` is not `/{tenant}/reports`
     * served under `/t`. Read as written, the template route still answers.
     */
    fun testTemplateRouteIsNotMatchedUnderAnAssumedPrefix() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject(
            "com/example/app/web/TenantController.java", """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class TenantController {
                @GetMapping("/{tenant}/reports")
                public String reports(@PathVariable("tenant") String tenant) {
                    return tenant;
                }
            }
            """.trimIndent()
        )

        val underPrefix = find("/t/acme/reports")
        assertEquals("The template must not absorb 'acme' under /t, got $underPrefix", 0, underPrefix["totalCount"].asInt())

        val asWritten = find("/acme/reports")
        assertEquals(listOf("/{tenant}/reports"), paths(asWritten["endpoints"]))
        assertTrue(asWritten["assumedPrefix"].isNull)
    }

    /**
     * A path settled for another verb is an answer about that URL: dropping one more segment to satisfy the method
     * filter would report a route the URL does not address.
     */
    fun testMethodFilterDoesNotPushTheLookupUnderAPrefix() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject(
            "com/example/app/web/BulkController.java", """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.PutMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class BulkController {
                @PutMapping("/items")
                public String replaceAll() {
                    return "ok";
                }
            }
            """.trimIndent()
        )

        val root = find("/api/demo/items", httpMethod = "PUT")

        assertEquals(
            "PUT /items is not what /api/demo/items addresses, got ${root["endpoints"]}",
            0, root["totalCount"].asInt()
        )
        assertTrue(root["assumedPrefix"].isNull)
    }

    /**
     * Spring binds a missing value to a parameter with a `defaultValue`, and to one it considers optional, instead
     * of rejecting the request. A client generated from `required: true` would make such a parameter mandatory.
     */
    fun testJavaParameterWithADefaultOrOptionalTypeIsNotRequired() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject(
            "com/example/app/web/WindowController.java", """
            package com.example.app.web;

            import java.util.Optional;
            import org.springframework.lang.Nullable;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class WindowController {
                @GetMapping("/api/java/activity")
                public String activity(
                        @RequestParam(defaultValue = "7d") String window,
                        @RequestParam(value = "from", required = true, defaultValue = "0") String from,
                        @RequestParam @Nullable String cursor,
                        @RequestParam Optional<String> sort,
                        @RequestParam String page,
                        @RequestHeader(value = "X-Trace", defaultValue = "none") String trace,
                        @RequestHeader("X-Tenant") String tenant) {
                    return window;
                }
            }
            """.trimIndent()
        )

        val parameters = contractParameters("/api/java/activity")

        assertEquals(
            mapOf(
                "window" to false, "from" to false, "cursor" to false, "sort" to false, "page" to true,
                "X-Trace" to false, "X-Tenant" to true,
            ),
            parameters.mapValues { it.value["required"].asBoolean() }
        )
        assertEquals("7d", parameters.getValue("window")["defaultValue"].asText())
    }

    /** Kotlin states optionality in the signature: a nullable type or a default value. */
    fun testKotlinNullableOrDefaultedParameterIsNotRequired() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        addKotlinActivityController()

        val parameters = contractParameters("/api/short-links/{id}/activity")

        assertEquals(
            mapOf("id" to true, "window" to false, "from" to false, "size" to false, "page" to true),
            parameters.mapValues { it.value["required"].asBoolean() }
        )
        assertEquals("7d", parameters.getValue("window")["defaultValue"].asText())
    }

    /**
     * The reported line is the one the declaration's name is on. A KDoc or a Javadoc is part of the declaration's
     * text, so its start is the first line of the comment - above the annotations and the signature, and not the
     * line `explyt_trace_spring_call_chain` names for the method.
     */
    fun testEndpointLineIsTheDeclarationLineRatherThanItsDocumentation() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        val source = addKotlinActivityController()

        val endpoint = find("/api/short-links/{id}/activity")["endpoints"].single()

        val expectedLine = source.lines().indexOfFirst { it.trim().startsWith("fun activity(") } + 1
        assertEquals(expectedLine, endpoint["line"].asInt())
    }

    fun testJavaEndpointLineSkipsItsJavadocAndAnnotations() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        val source = """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class DocumentedController {
                /**
                 * Returns the status.
                 */
                @GetMapping("/api/documented/status")
                public String status() {
                    return "ok";
                }
            }
            """.trimIndent()
        myFixture.addFileToProject("com/example/app/web/DocumentedController.java", source)

        val endpoint = find("/api/documented/status")["endpoints"].single()

        val expectedLine = source.lines().indexOfFirst { it.contains("public String status()") } + 1
        assertEquals(expectedLine, endpoint["line"].asInt())
    }

    /**
     * A Kotlin `suspend` handler compiles to a method with the coroutine continuation as one more, last parameter and
     * the return type `Object`. Spring supplies the continuation and answers with the declared type, so reporting the
     * compiled shape shows a request argument no client sends and a response nobody can generate a client for.
     */
    fun testSuspendHandlerIsReportedWithItsDeclaredSignature() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        addSuspendController()
        val compiled = suspendHandler("find")
        assertEquals(
            "The compiled handler carries the continuation this test is about",
            listOf("code", "principal", "\$completion"),
            compiled.parameterList.parameters.map { it.name }
        )
        assertEquals(CommonClassNames.JAVA_LANG_OBJECT, compiled.returnType?.canonicalText)

        val found = find("/api/suspend/abc")["endpoints"].single()
        val listed = mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), controllerFilter = "SuspendController")
        )["endpoints"].single { it["methodName"].asText() == "find" }
        val contract = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/api/suspend/abc", projectPath = projectPath())
        )["endpoints"].single()

        for ((tool, endpoint) in listOf("find" to found, "list" to listed, "contract" to contract)) {
            assertEquals("$tool parameters", listOf("code", "principal"), endpoint["parameters"].map { it["name"].asText() })
            assertEquals(
                "$tool return type",
                "org.springframework.http.ResponseEntity<java.lang.String>",
                endpoint["returnType"].asText()
            )
        }
        assertEquals("COMPLETE", contract["contractStatus"].asText())
        assertEquals("FRAMEWORK", contract["parameters"].single { it["name"].asText() == "principal" }["source"].asText())
    }

    /** A `suspend` handler declared to return `Unit` answers with no body, like a non-suspend Kotlin `Unit` function. */
    fun testSuspendUnitHandlerReturnsVoid() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        addSuspendController()

        val endpoint = find("/api/suspend/abc/touch")["endpoints"].single()

        assertEquals("void", endpoint["returnType"].asText())
        assertEquals(listOf("code"), endpoint["parameters"].map { it["name"].asText() })
    }

    /**
     * Two handlers sharing a path and a verb are chosen by the request's `Content-Type`, not by their order: without
     * `consumes` on the records they read as one mapping declared twice.
     */
    fun testHandlersSharingAPathCarryTheirConsumes() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject(
            "com/example/app/web/OrderImportController.kt", """
            package com.example.app.web

            import org.springframework.http.MediaType
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RequestBody
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class OrderImportController {
                @PostMapping("/api/orders/import", consumes = [MediaType.APPLICATION_JSON_VALUE])
                fun importJson(@RequestBody orders: List<String>): Int = orders.size

                @PostMapping(
                    "/api/orders/import",
                    consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
                    produces = [MediaType.TEXT_PLAIN_VALUE],
                )
                fun importCsv(@RequestBody csv: String): Int = csv.length
            }
            """.trimIndent()
        )
        val expected = mapOf(
            "importJson" to (listOf("application/json") to emptyList()),
            "importCsv" to (listOf("multipart/form-data") to listOf("text/plain")),
        )

        val found = find("/api/orders/import", httpMethod = "POST")["endpoints"]
        val listed = mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), controllerFilter = "OrderImportController")
        )["endpoints"]
        val compact = mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), controllerFilter = "OrderImportController", compact = true)
        )["endpoints"]
        val contract = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/api/orders/import", projectPath = projectPath(), httpMethod = "POST")
        )["endpoints"]

        for ((tool, endpoints) in listOf("find" to found, "list" to listed, "compact" to compact, "contract" to contract)) {
            assertEquals(
                "$tool consumes/produces",
                expected,
                endpoints.associate { it["methodName"].asText() to (mediaTypes(it, "consumes") to mediaTypes(it, "produces")) }
            )
        }
        for ((tool, endpoints) in listOf("find" to found, "compact" to compact)) {
            assertFalse(
                "A mapping declaring no media type has no 'produces' key in a $tool record, got ${endpoints.toList()}",
                endpoints.single { it["methodName"].asText() == "importJson" }.has("produces")
            )
        }
        assertTrue(
            "The contract always carries both arrays",
            contract.all { it["consumes"].isArray && it["produces"].isArray }
        )
    }

    private fun mediaTypes(endpoint: JsonNode, field: String): List<String> =
        endpoint[field]?.map { it.asText() }.orEmpty()

    private fun addSuspendController() {
        myFixture.addFileToProject(
            "com/example/app/web/SuspendController.kt", """
            package com.example.app.web

            import java.security.Principal
            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class SuspendController {
                @GetMapping("/api/suspend/{code}")
                suspend fun find(@PathVariable code: String, principal: Principal): ResponseEntity<String> =
                    ResponseEntity.ok(code)

                @PostMapping("/api/suspend/{code}/touch")
                suspend fun touch(@PathVariable code: String) {
                }
            }
            """.trimIndent()
        )
    }

    private fun suspendHandler(name: String): PsiMethod =
        JavaPsiFacade.getInstance(project)
            .findClass("com.example.app.web.SuspendController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName(name, false).single()

    private suspend fun contractParameters(url: String): Map<String, JsonNode> =
        mapper.readTree(toolset.getEndpointContract(urlPattern = url, projectPath = projectPath()))["endpoints"]
            .single()["parameters"]
            .associateBy { it["name"].asText() }

    private fun addKotlinActivityController(): String {
        val source = """
            package com.example.app.web

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("/api/short-links")
            class ShortLinkAdminController {

                /**
                 * Activity of one short link over a window.
                 *
                 * The window defaults to a week.
                 */
                @GetMapping("/{id}/activity")
                fun activity(
                    @PathVariable id: Long,
                    @RequestParam(defaultValue = "7d") window: String,
                    @RequestParam from: String?,
                    @RequestParam size: Int = 20,
                    @RequestParam page: Int,
                ): String = window
            }
        """.trimIndent()
        myFixture.addFileToProject("com/example/app/web/ShortLinkAdminController.kt", source)
        return source
    }
}
