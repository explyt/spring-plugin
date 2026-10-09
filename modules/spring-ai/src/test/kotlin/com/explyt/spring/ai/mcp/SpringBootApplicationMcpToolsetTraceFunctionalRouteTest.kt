/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.toUElementOfType
import java.io.File

/**
 * Where `explyt_trace_spring_call_chain` starts when the line it is given registers a functional route (#505).
 *
 * A functional route has no handler method of its own: `GET("/x", handler::list)` delegates to a method of another
 * bean, and `GET("/x") { ... }` handles the request in a lambda. Both sit inside the `@Bean` factory that registers
 * every route of the router, so starting from the enclosing method traced all routes at once and matched no test
 * request, because no endpoint is declared by the factory method itself.
 *
 * The expected start is the route's own handler: the referenced method for a callable reference, and for a lambda the
 * registering factory restricted to the calls written inside that lambda. Uses the heavy
 * [JavaCodeInsightFixtureTestCase] because `traceCallChain` resolves its file through `LocalFileSystem`.
 */
class SpringBootApplicationMcpToolsetTraceFunctionalRouteTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    override fun setUp() {
        super.setUp()
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            LIBRARIES.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        addSource(MAIN_ROOT, CATALOG_FILE, CATALOG_SOURCE)
        addSource(MAIN_ROOT, INVENTORY_FILE, INVENTORY_SOURCE)
        addSource(MAIN_ROOT, "com/example/routes/orders/OrderService.java", ORDER_SERVICE_SOURCE)
        addSource(MAIN_ROOT, "com/example/routes/orders/OrderHandler.java", ORDER_HANDLER_SOURCE)
        addSource(MAIN_ROOT, ORDER_ROUTES_FILE, ORDER_ROUTES_SOURCE)
        addSource(MAIN_ROOT, PLAIN_FILE, PLAIN_SOURCE)
        addSource(TEST_ROOT, CATALOG_TEST_FILE, CATALOG_TEST_SOURCE, isTestSource = true)
        addSource(TEST_ROOT, INVENTORY_TEST_FILE, INVENTORY_TEST_SOURCE, isTestSource = true)
        addSource(TEST_ROOT, ORDER_TEST_FILE, ORDER_TEST_SOURCE, isTestSource = true)
        addSource(TEST_ROOT, PLAIN_TEST_FILE, PLAIN_TEST_SOURCE, isTestSource = true)
    }

    fun testCoRouterReferenceRouteStartsAtItsHandler() = runBlocking<Unit> {
        assertRouteModelled("/co/items", "GET")

        val chain = traceAt(CATALOG_FILE, CATALOG_SOURCE, "GET(\"/co/items\", handler::list)")["chain"]

        assertEquals("CatalogHandler.list", nameOf(chain[0]))
        assertEquals(listOf("CatalogService.list"), targetsOf(chain[0]))
    }

    fun testRouterReferenceRouteStartsAtItsHandler() = runBlocking<Unit> {
        assertRouteModelled("/mvc/items", "GET")

        val chain = traceAt(INVENTORY_FILE, INVENTORY_SOURCE, "GET(\"/mvc/items\", handler::list)")["chain"]

        assertEquals("InventoryHandler.list", nameOf(chain[0]))
        assertEquals(listOf("InventoryService.list"), targetsOf(chain[0]))
    }

    fun testJavaBuilderReferenceRouteStartsAtItsHandler() = runBlocking<Unit> {
        assertRouteModelled("/java/orders", "GET")

        val chain = traceAt(ORDER_ROUTES_FILE, ORDER_ROUTES_SOURCE, ".GET(\"/java/orders\", handler::list)")["chain"]

        assertEquals("OrderHandler.list", nameOf(chain[0]))
        assertEquals(listOf("OrderService.list"), targetsOf(chain[0]))
    }

    fun testCoRouterLambdaRouteTracesOnlyItsOwnCalls() = runBlocking<Unit> {
        assertRouteModelled("/co/items/one", "GET")

        val chain = traceAt(CATALOG_FILE, CATALOG_SOURCE, "GET(\"/co/items/one\")")["chain"]

        assertLambdaRouteStart(chain, factory = "CatalogRoutes.catalogRoutes", ownCall = "CatalogService.find")
    }

    fun testRouterLambdaRouteTracesOnlyItsOwnCalls() = runBlocking<Unit> {
        assertRouteModelled("/mvc/items/one", "GET")

        val chain = traceAt(INVENTORY_FILE, INVENTORY_SOURCE, "GET(\"/mvc/items/one\")")["chain"]

        assertLambdaRouteStart(chain, factory = "InventoryRoutes.inventoryRoutes", ownCall = "InventoryService.find")
    }

    fun testJavaBuilderLambdaRouteTracesOnlyItsOwnCalls() = runBlocking<Unit> {
        assertRouteModelled("/java/orders/one", "GET")

        val chain = traceAt(ORDER_ROUTES_FILE, ORDER_ROUTES_SOURCE, ".GET(\"/java/orders/one\"")["chain"]

        assertLambdaRouteStart(chain, factory = "OrderRoutes.orderRoutes", ownCall = "OrderService.find")
    }

    fun testCoRouterReferenceRouteListsTestsCallingItsUrl() = runBlocking<Unit> {
        val head = traceAt(CATALOG_FILE, CATALOG_SOURCE, "GET(\"/co/items\", handler::list)", includeTests = true)["chain"][0]

        assertUrlReferences(head, CATALOG_TEST_FILE, "/co/items", CATALOG_TEST_SOURCE, "get().uri(\"/co/items\")")
    }

    fun testRouterReferenceRouteListsTestsCallingItsUrl() = runBlocking<Unit> {
        val head = traceAt(INVENTORY_FILE, INVENTORY_SOURCE, "GET(\"/mvc/items\", handler::list)", includeTests = true)["chain"][0]

        assertUrlReferences(head, INVENTORY_TEST_FILE, "/mvc/items", INVENTORY_TEST_SOURCE, "get(\"/mvc/items\")")
    }

    fun testJavaBuilderReferenceRouteListsTestsCallingItsUrl() = runBlocking<Unit> {
        val head = traceAt(ORDER_ROUTES_FILE, ORDER_ROUTES_SOURCE, ".GET(\"/java/orders\", handler::list)", includeTests = true)["chain"][0]

        assertUrlReferences(head, ORDER_TEST_FILE, "/java/orders", ORDER_TEST_SOURCE, "get().uri(\"/java/orders\")")
    }

    fun testCoRouterLambdaRouteListsTestsCallingItsUrl() = runBlocking<Unit> {
        val head = traceAt(CATALOG_FILE, CATALOG_SOURCE, "GET(\"/co/items/one\")", includeTests = true)["chain"][0]

        assertUrlReferences(head, CATALOG_TEST_FILE, "/co/items/one", CATALOG_TEST_SOURCE, "get().uri(\"/co/items/one\")")
    }

    fun testAnnotatedHandlerTraceIsUnchanged() = runBlocking<Unit> {
        assertRouteModelled("/plain/items", "GET")

        val head = traceAt(PLAIN_FILE, PLAIN_SOURCE, "fun items(): String = service.items()", includeTests = true)["chain"][0]

        assertEquals("PlainController.items", nameOf(head))
        assertEquals(listOf("PlainService.items"), targetsOf(head))
        assertUrlReferences(head, PLAIN_TEST_FILE, "/plain/items", PLAIN_TEST_SOURCE, "get(\"/plain/items\")")
    }

    fun testCoRouterLambdaRouteStartIsMarkedWithItsRoute() = runBlocking<Unit> {
        val head = traceAt(CATALOG_FILE, CATALOG_SOURCE, "GET(\"/co/items/one\")")["chain"][0]

        assertEquals("GET /co/items/one", head["route"]?.asText())
    }

    fun testJavaBuilderReferenceRouteStartIsMarkedWithItsRoute() = runBlocking<Unit> {
        val head = traceAt(ORDER_ROUTES_FILE, ORDER_ROUTES_SOURCE, ".GET(\"/java/orders\", handler::list)")["chain"][0]

        assertEquals("GET /java/orders", head["route"]?.asText())
    }

    fun testRouteIsAbsentOutsideFunctionalRoutes() = runBlocking<Unit> {
        val chain = traceAt(PLAIN_FILE, PLAIN_SOURCE, "fun items(): String = service.items()")["chain"]

        assertEquals("PlainController.items", nameOf(chain[0]))
        assertTrue("No node of an annotated handler trace carries a route, got $chain", chain.none { it.has("route") })
    }

    fun testNestedRouteKeepsItsFullPathAndRequest() = runBlocking<Unit> {
        val source = CATALOG_SOURCE.replace(
            "GET(\"/co/items\", handler::list)",
            "\"/a\".nest { GET(\"/b\", handler::list) }"
        )
        val requests = CATALOG_TEST_SOURCE.replace("/co/items\"", "/a/b\"")
        addSource(MAIN_ROOT, CATALOG_FILE, source)
        addSource(TEST_ROOT, CATALOG_TEST_FILE, requests, isTestSource = true)
        assertRouteModelled("/a/b", "GET")

        val head = traceAt(CATALOG_FILE, source, "GET(\"/b\"", includeTests = true)["chain"][0]

        assertEquals("CatalogHandler.list", nameOf(head))
        assertEquals("GET /a/b", head["route"]?.asText())
        assertUrlReferences(head, CATALOG_TEST_FILE, "/a/b", requests, "get().uri(\"/a/b\")")
    }

    fun testPathPredicateNestTraceStartsWithThePrefixedRoute() = runBlocking<Unit> {
        val source = CATALOG_SOURCE.replace(
            "GET(\"/co/items\", handler::list)",
            """
            path("/a").nest {
                GET("/b", handler::list)
            }
            """.trimIndent()
        )
        addSource(MAIN_ROOT, CATALOG_FILE, source)
        assertDslPathResolves("org.springframework.web.reactive.function.server.CoRouterFunctionDsl")
        assertRouteModelled("/a/b", "GET")

        val head = traceAt(CATALOG_FILE, source, "GET(\"/b\"")["chain"][0]

        assertEquals("CatalogHandler.list", nameOf(head))
        assertEquals("GET /a/b", head["route"]?.asText())
        assertEquals(listOf("CatalogService.list"), targetsOf(head))
    }

    fun testReactiveRouterPathPredicateNestTraceStartsWithThePrefixedRoute() = runBlocking<Unit> {
        val source = CATALOG_SOURCE
            .replace("import org.springframework.web.reactive.function.server.coRouter", "import org.springframework.web.reactive.function.server.router")
            .replace("= coRouter {", "= router {")
            .replace("suspend fun list(request: ServerRequest): ServerResponse", "fun list(request: ServerRequest): reactor.core.publisher.Mono<ServerResponse>")
            .replace("buildAndAwait()", "build()")
        assertCatalogNestTrace(
            "path(\"/a\").nest { GET(\"/b\", handler::list) }",
            "/a/b",
            source,
            "org.springframework.web.reactive.function.server.RouterFunctionDsl"
        )
    }

    fun testValueArgumentPathPredicateNestTraceStartsWithThePrefixedRoute() = runBlocking<Unit> {
        assertCatalogNestTrace(
            "nest(path(\"/a\")) { GET(\"/b\", handler::list) }",
            "/a/b",
            dslClass = "org.springframework.web.reactive.function.server.CoRouterFunctionDsl"
        )
    }

    fun testStringNestTraceStartsWithThePrefixedRoute() = runBlocking<Unit> {
        assertCatalogNestTrace("\"/a\".nest { GET(\"/b\", handler::list) }", "/a/b")
    }

    fun testAcceptPredicateNestTraceKeepsTheChildRoute() = runBlocking<Unit> {
        assertCatalogNestTrace(
            "accept(org.springframework.http.MediaType.APPLICATION_JSON).nest { GET(\"/b\", handler::list) }",
            "/b"
        )
    }

    fun testJavaBuilderPathNestTraceHasNoInventedRoute() = runBlocking<Unit> {
        val source = ORDER_ROUTES_SOURCE.replace(
            ".GET(\"/java/orders\", handler::list)",
            ".path(\"/a\", b -> b.GET(\"/b\", handler::list))"
        )
        addSource(MAIN_ROOT, ORDER_ROUTES_FILE, source)
        assertRouteModelled("/a/b", "GET")

        val head = traceAt(ORDER_ROUTES_FILE, source, "b.GET(\"/b\", handler::list)")["chain"][0]

        assertEquals("OrderHandler.list", nameOf(head))
        assertFalse("An unresolved Java nesting prefix leaves the route unknown, got ${head["route"]}", head.has("route"))
        assertEquals(listOf("OrderService.list"), targetsOf(head))
    }

    private suspend fun assertCatalogNestTrace(
        registration: String,
        path: String,
        template: String = CATALOG_SOURCE,
        dslClass: String? = null,
    ) {
        val source = template.replace("GET(\"/co/items\", handler::list)", registration)
        val requests = CATALOG_TEST_SOURCE.replace("/co/items\"", "$path\"")
        addSource(MAIN_ROOT, CATALOG_FILE, source)
        addSource(TEST_ROOT, CATALOG_TEST_FILE, requests, isTestSource = true)
        dslClass?.let(::assertDslPathResolves)
        assertRouteModelled(path, "GET")

        val head = traceAt(CATALOG_FILE, source, "GET(\"/b\"", includeTests = true)["chain"][0]

        assertEquals("CatalogHandler.list", nameOf(head))
        assertEquals("GET $path", head["route"]?.asText())
        assertEquals(listOf("CatalogService.list"), targetsOf(head))
        assertUrlReferences(head, CATALOG_TEST_FILE, path, requests, "get().uri(\"$path\")")
    }

    private fun assertDslPathResolves(dslClass: String) = ReadAction.run<Throwable> {
        val file = LocalFileSystem.getInstance().findFileByPath("${project.basePath}/$MAIN_ROOT/$CATALOG_FILE")!!
        val psiFile = PsiManager.getInstance(project).findFile(file)!!
        val call = PsiTreeUtil.findChildrenOfType(psiFile, KtCallExpression::class.java)
            .single { it.calleeExpression?.text == "path" }
            .toUElementOfType<UCallExpression>()!!
        val method = call.resolve()
        assertNotNull("Precondition: path resolves to a DSL member", method)
        assertEquals(dslClass, method!!.containingClass?.qualifiedName)
        assertEquals(listOf("java.lang.String"), method.parameterList.parameters.map { it.type.canonicalText })
    }

    fun testGenericMethodRouteKeepsItsVerbAndRequest() = runBlocking<Unit> {
        val source = CATALOG_SOURCE.replace(
            "GET(\"/co/items\", handler::list)",
            "\"/created\".nest { method(org.springframework.http.HttpMethod.POST, handler::list) }"
        )
        val requests = CATALOG_TEST_SOURCE.replace("get().uri(\"/co/items\")", "post().uri(\"/created\")")
        addSource(MAIN_ROOT, CATALOG_FILE, source)
        addSource(TEST_ROOT, CATALOG_TEST_FILE, requests, isTestSource = true)
        assertRouteModelled("/created", "POST")

        val head = traceAt(CATALOG_FILE, source, "method(org.springframework.http.HttpMethod.POST", includeTests = true)["chain"][0]

        assertEquals("CatalogHandler.list", nameOf(head))
        assertEquals("POST /created", head["route"]?.asText())
        assertUrlReferences(head, CATALOG_TEST_FILE, "/created", requests, "post().uri(\"/created\")")
    }

    fun testIteratedPathsListBothRequestsAndMarkTheFirstPath() = runBlocking<Unit> {
        val source = CATALOG_SOURCE.replace(
            "GET(\"/co/items\", handler::list)",
            "listOf(\"/x\", \"/y\").forEach { path -> GET(path, handler::list) }"
        )
        val requests = CATALOG_TEST_SOURCE.replace("/co/items\"", "/x\"")
            .replace("/co/items/one", "/y")
        addSource(MAIN_ROOT, CATALOG_FILE, source)
        addSource(TEST_ROOT, CATALOG_TEST_FILE, requests, isTestSource = true)
        assertRouteModelled("/x", "GET")
        assertRouteModelled("/y", "GET")

        val head = traceAt(CATALOG_FILE, source, "GET(path, handler::list)", includeTests = true)["chain"][0]

        assertEquals("CatalogHandler.list", nameOf(head))
        assertEquals("GET /x", head["route"]?.asText())
        val references = head["testUrlReferences"]
        assertEquals(listOf("/x", "/y"), references.map { it["endpointPath"].asText() }.sorted())
        assertEquals(listOf("$TEST_ROOT/$CATALOG_TEST_FILE"), references.map { it["filePath"].asText() }.distinct())
        assertEquals(
            listOf(lineOf(requests, "get().uri(\"/x\")"), lineOf(requests, "get().uri(\"/y\")")),
            references.flatMap { it["lines"].map { line -> line.asInt() } }.sorted()
        )
    }

    fun testJavaHandlerFieldFallsBackToTheRegistrationMethod() = runBlocking<Unit> {
        val source = ORDER_ROUTES_SOURCE.replace(
            "public class OrderRoutes {",
            """public class OrderRoutes {
                private final org.springframework.web.reactive.function.server.HandlerFunction<ServerResponse> handlerFn = request -> ServerResponse.ok().build();"""
        ).replace(".GET(\"/java/orders\", handler::list)", ".GET(\"/f\", handlerFn)")
        addSource(MAIN_ROOT, ORDER_ROUTES_FILE, source)
        assertRouteModelled("/f", "GET")

        val head = traceAt(ORDER_ROUTES_FILE, source, ".GET(\"/f\", handlerFn)")["chain"][0]

        assertEquals("OrderRoutes.orderRoutes", nameOf(head))
        assertFalse("An opaque handler field supplies no resolved route target", head.has("route"))
        assertEquals(listOf("OrderService.find", "OrderService.remove"), targetsOf(head))
    }

    fun testJavaRouteNestedUnderAPredicatePrefixHasNoInventedRoute() = runBlocking<Unit> {
        assertNestedJavaRouteHasNoInventedRoute(
            ".nest(org.springframework.web.reactive.function.server.RequestPredicates.path(\"/a\"), b -> b.GET(\"/x\", handler::list))"
        )
    }

    fun testJavaRouteNestedUnderABuilderPathHasNoInventedRoute() = runBlocking<Unit> {
        assertNestedJavaRouteHasNoInventedRoute(".path(\"/a\", b -> b.GET(\"/x\", handler::list))")
    }

    fun testRouteWithoutAPathHasNoTestUrlReferences() = runBlocking<Unit> {
        val source = CATALOG_SOURCE.replace(
            "GET(\"/co/items/one\") { catalog.find()",
            "GET { catalog.find()"
        )
        addSource(MAIN_ROOT, CATALOG_FILE, source)

        val head = traceAt(CATALOG_FILE, source, "GET { catalog.find()", includeTests = true)["chain"][0]

        assertEquals("CatalogRoutes.catalogRoutes", nameOf(head))
        assertEquals(listOf("CatalogService.find"), targetsOf(head))
        assertFalse("A route without a known path has no route, got ${head["route"]}", head.has("route"))
        assertFalse(
            "Requests cannot be matched to a route without a known path, got ${head["testUrlReferences"]}",
            head.has("testUrlReferences")
        )
    }

    fun testLineInsideALambdaRouteBodyStartsAtThatRoute() = runBlocking<Unit> {
        val source = CATALOG_SOURCE.replace(
            "GET(\"/co/items/one\") { catalog.find(); ServerResponse.ok().buildAndAwait() }",
            "GET(\"/co/items/one\") {\n        catalog.find()\n        ServerResponse.ok().buildAndAwait()\n    }"
        )
        addSource(MAIN_ROOT, CATALOG_FILE, source)

        val chain = traceAt(CATALOG_FILE, source, "catalog.find()")["chain"]

        assertLambdaRouteStart(chain, factory = "CatalogRoutes.catalogRoutes", ownCall = "CatalogService.find")
        assertEquals("GET /co/items/one", chain[0]["route"]?.asText())
    }

    fun testNestHeaderLineFallsBackToTheFactory() = runBlocking<Unit> {
        val source = CATALOG_SOURCE.replace(
            "GET(\"/co/items\", handler::list)",
            "\"/a\".nest {\n        GET(\"/b\", handler::list)\n    }"
        )
        addSource(MAIN_ROOT, CATALOG_FILE, source)

        val head = traceAt(CATALOG_FILE, source, "\"/a\".nest {")["chain"][0]

        assertEquals("CatalogRoutes.catalogRoutes", nameOf(head))
        assertFalse(head.has("route"))
        assertTrue("The whole factory is traced, got ${targetsOf(head)}", "CatalogService.remove" in targetsOf(head))
    }

    private suspend fun assertNestedJavaRouteHasNoInventedRoute(nestedRoute: String) {
        val source = ORDER_ROUTES_SOURCE.replace(".GET(\"/java/orders\", handler::list)", nestedRoute)
        val requests = ORDER_TEST_SOURCE.replace("get().uri(\"/java/orders\")", "get().uri(\"/x\")")
        addSource(MAIN_ROOT, ORDER_ROUTES_FILE, source)
        addSource(TEST_ROOT, ORDER_TEST_FILE, requests, isTestSource = true)

        val head = traceAt(ORDER_ROUTES_FILE, source, "b.GET(\"/x\", handler::list)", includeTests = true)["chain"][0]

        assertEquals("OrderHandler.list", nameOf(head))
        assertFalse("The unresolved prefix leaves the route unknown, got ${head["route"]}", head.has("route"))
        val toX = head["testUrlReferences"]?.filter { it["endpointPath"].asText() == "/x" }.orEmpty()
        assertTrue("No request is matched to the prefix-less path, got $toX", toX.isEmpty())
    }

    private fun assertLambdaRouteStart(chain: JsonNode, factory: String, ownCall: String) {
        val names = chain.map(::nameOf)
        assertEquals("The lambda is reported under the factory that registers it, got $names", factory, names.first())
        assertEquals(
            "Only the calls written inside the route's lambda belong to the route, got ${targetsOf(chain[0])}",
            listOf(ownCall), targetsOf(chain[0])
        )
        assertEquals(
            "Methods reached through other routes of the same factory are not part of this route",
            listOf(factory, ownCall), names
        )
    }

    private fun assertUrlReferences(
        head: JsonNode,
        testFile: String,
        endpointPath: String,
        testSource: String,
        requestAnchor: String,
    ) {
        val byUrl = head["testUrlReferences"]
        assertNotNull("The start node of a route must carry testUrlReferences", byUrl)
        val inTestFile = byUrl.filter { it["filePath"].asText() == "$TEST_ROOT/$testFile" }
        assertEquals("Exactly the requests to $endpointPath are listed, got $byUrl", 1, inTestFile.size)
        assertEquals(endpointPath, inTestFile.single()["endpointPath"].asText())
        assertEquals(listOf(lineOf(testSource, requestAnchor)), inTestFile.single()["lines"].map { it.asInt() })
    }

    private suspend fun assertRouteModelled(path: String, httpMethod: String) {
        val endpoints = mapper.readTree(
            toolset.findEndpoint(urlPattern = path, projectPath = project.basePath!!, httpMethod = httpMethod)
        )["endpoints"]
        assertTrue(
            "Precondition: the endpoint model holds $httpMethod $path, got $endpoints",
            endpoints.any { it["fullPath"].asText() == path }
        )
    }

    private suspend fun traceAt(file: String, source: String, anchor: String, includeTests: Boolean = false): JsonNode =
        mapper.readTree(
            toolset.traceCallChain(
                filePath = "$MAIN_ROOT/$file",
                line = lineOf(source, anchor),
                projectPath = project.basePath!!,
                depth = 3,
                includeTests = includeTests,
            )
        )

    private fun nameOf(node: JsonNode): String =
        "${node["className"].asText().substringAfterLast('.')}.${node["methodName"].asText()}"

    private fun targetsOf(node: JsonNode): List<String> = node["callsInto"].map { it["target"].asText() }

    private fun lineOf(source: String, anchor: String): Int {
        val index = source.lines().indexOfFirst { it.contains(anchor) }
        assertTrue("Anchor '$anchor' is absent from the fixture", index >= 0)
        return index + 1
    }

    private fun addSource(rootName: String, relativePath: String, content: String, isTestSource: Boolean = false) {
        val sourcesRoot = File(project.basePath!!, rootName).apply { mkdirs() }
        File(sourcesRoot, relativePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        val sourcesRootVf = VfsUtil.findFile(sourcesRoot.toPath(), true)
            ?: error("Sources root not visible in VFS: ${sourcesRoot.absolutePath}")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            val alreadyRegistered = model.contentEntries.any { it.file == sourcesRootVf }
            if (!alreadyRegistered) model.addContentEntry(sourcesRootVf).addSourceFolder(sourcesRootVf, isTestSource)
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private companion object {
        const val MAIN_ROOT = "routesMain"
        const val TEST_ROOT = "routesTest"
        const val CATALOG_FILE = "com/example/routes/catalog/Catalog.kt"
        const val INVENTORY_FILE = "com/example/routes/inventory/Inventory.kt"
        const val ORDER_ROUTES_FILE = "com/example/routes/orders/OrderRoutes.java"
        const val PLAIN_FILE = "com/example/routes/plain/Plain.kt"
        const val CATALOG_TEST_FILE = "com/example/routes/catalog/CatalogRoutesTest.kt"
        const val INVENTORY_TEST_FILE = "com/example/routes/inventory/InventoryRoutesTest.kt"
        const val ORDER_TEST_FILE = "com/example/routes/orders/OrderRoutesTest.kt"
        const val PLAIN_TEST_FILE = "com/example/routes/plain/PlainControllerTest.kt"

        val LIBRARIES = listOf(
            TestLibrary.springWebMvc_6_0_7,
            TestLibrary.springReactiveWeb_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.kotlin_1_9_22,
            TestLibrary.kotlin_coroutines_1_7_1,
        )

        val CATALOG_SOURCE = """
            package com.example.routes.catalog

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
            import org.springframework.stereotype.Service
            import org.springframework.web.reactive.function.server.RouterFunction
            import org.springframework.web.reactive.function.server.ServerRequest
            import org.springframework.web.reactive.function.server.ServerResponse
            import org.springframework.web.reactive.function.server.buildAndAwait
            import org.springframework.web.reactive.function.server.coRouter

            @Service
            class CatalogService {
                fun list(): String = "items"
                fun find(): String = "item"
                fun remove(): String = "removed"
            }

            @Component
            class CatalogHandler(private val catalog: CatalogService) {
                suspend fun list(request: ServerRequest): ServerResponse {
                    catalog.list()
                    return ServerResponse.ok().buildAndAwait()
                }
            }

            @Configuration
            class CatalogRoutes {
                @Bean
                fun catalogRoutes(handler: CatalogHandler, catalog: CatalogService): RouterFunction<ServerResponse> = coRouter {
                    GET("/co/items", handler::list)
                    GET("/co/items/one") { catalog.find(); ServerResponse.ok().buildAndAwait() }
                    DELETE("/co/items") { catalog.remove(); ServerResponse.ok().buildAndAwait() }
                }
            }
        """.trimIndent()

        val INVENTORY_SOURCE = """
            package com.example.routes.inventory

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.stereotype.Component
            import org.springframework.stereotype.Service
            import org.springframework.web.servlet.function.RouterFunction
            import org.springframework.web.servlet.function.ServerRequest
            import org.springframework.web.servlet.function.ServerResponse
            import org.springframework.web.servlet.function.router

            @Service
            class InventoryService {
                fun list(): String = "items"
                fun find(): String = "item"
                fun remove(): String = "removed"
            }

            @Component
            class InventoryHandler(private val inventory: InventoryService) {
                fun list(request: ServerRequest): ServerResponse {
                    inventory.list()
                    return ServerResponse.ok().build()
                }
            }

            @Configuration
            class InventoryRoutes {
                @Bean
                fun inventoryRoutes(handler: InventoryHandler, inventory: InventoryService): RouterFunction<ServerResponse> = router {
                    GET("/mvc/items", handler::list)
                    GET("/mvc/items/one") { inventory.find(); ServerResponse.ok().build() }
                    DELETE("/mvc/items") { inventory.remove(); ServerResponse.ok().build() }
                }
            }
        """.trimIndent()

        val ORDER_SERVICE_SOURCE = """
            package com.example.routes.orders;

            import org.springframework.stereotype.Service;

            @Service
            public class OrderService {
                public String list() { return "orders"; }
                public String find() { return "order"; }
                public String remove() { return "removed"; }
            }
        """.trimIndent()

        val ORDER_HANDLER_SOURCE = """
            package com.example.routes.orders;

            import org.springframework.stereotype.Component;
            import org.springframework.web.reactive.function.server.ServerRequest;
            import org.springframework.web.reactive.function.server.ServerResponse;
            import reactor.core.publisher.Mono;

            @Component
            public class OrderHandler {
                private final OrderService orders;

                public OrderHandler(OrderService orders) {
                    this.orders = orders;
                }

                public Mono<ServerResponse> list(ServerRequest request) {
                    orders.list();
                    return ServerResponse.ok().build();
                }
            }
        """.trimIndent()

        val ORDER_ROUTES_SOURCE = """
            package com.example.routes.orders;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.web.reactive.function.server.RouterFunction;
            import org.springframework.web.reactive.function.server.RouterFunctions;
            import org.springframework.web.reactive.function.server.ServerResponse;

            @Configuration
            public class OrderRoutes {
                @Bean
                public RouterFunction<ServerResponse> orderRoutes(OrderHandler handler, OrderService orders) {
                    return RouterFunctions.route()
                            .GET("/java/orders", handler::list)
                            .GET("/java/orders/one", request -> { orders.find(); return ServerResponse.ok().build(); })
                            .DELETE("/java/orders", request -> { orders.remove(); return ServerResponse.ok().build(); })
                            .build();
                }
            }
        """.trimIndent()

        val PLAIN_SOURCE = """
            package com.example.routes.plain

            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @Service
            class PlainService {
                fun items(): String = "items"
            }

            @RestController
            class PlainController(private val service: PlainService) {
                @GetMapping("/plain/items")
                fun items(): String = service.items()
            }
        """.trimIndent()

        val CATALOG_TEST_SOURCE = """
            package com.example.routes.catalog

            import org.springframework.test.web.reactive.server.WebTestClient

            class CatalogRoutesTest {
                fun listsItems(client: WebTestClient) {
                    client.get().uri("/co/items").exchange()
                }

                fun findsOne(client: WebTestClient) {
                    client.get().uri("/co/items/one").exchange()
                }

                fun removesItems(client: WebTestClient) {
                    client.delete().uri("/co/items").exchange()
                }
            }
        """.trimIndent()

        val INVENTORY_TEST_SOURCE = """
            package com.example.routes.inventory

            import org.springframework.test.web.servlet.MockMvc
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class InventoryRoutesTest {
                fun listsItems(mockMvc: MockMvc) {
                    mockMvc.perform(get("/mvc/items"))
                }

                fun findsOne(mockMvc: MockMvc) {
                    mockMvc.perform(get("/mvc/items/one"))
                }

                fun removesItems(mockMvc: MockMvc) {
                    mockMvc.perform(delete("/mvc/items"))
                }
            }
        """.trimIndent()

        val ORDER_TEST_SOURCE = """
            package com.example.routes.orders

            import org.springframework.test.web.reactive.server.WebTestClient

            class OrderRoutesTest {
                fun listsOrders(client: WebTestClient) {
                    client.get().uri("/java/orders").exchange()
                }

                fun findsOne(client: WebTestClient) {
                    client.get().uri("/java/orders/one").exchange()
                }

                fun removesOrders(client: WebTestClient) {
                    client.delete().uri("/java/orders").exchange()
                }
            }
        """.trimIndent()

        val PLAIN_TEST_SOURCE = """
            package com.example.routes.plain

            import org.springframework.test.web.servlet.MockMvc
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class PlainControllerTest {
                fun listsItems(mockMvc: MockMvc) {
                    mockMvc.perform(get("/plain/items"))
                }
            }
        """.trimIndent()
    }
}
