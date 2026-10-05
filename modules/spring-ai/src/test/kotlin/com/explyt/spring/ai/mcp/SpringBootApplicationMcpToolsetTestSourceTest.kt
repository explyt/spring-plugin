/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import kotlinx.coroutines.runBlocking

/**
 * An endpoint declared in a test source root is reported by every endpoint tool, marked `testSource: true`, and
 * listed after every production endpoint - a probe controller nested in a test class answered a production URL
 * as if it were the handler that dispatches it.
 */
class SpringBootApplicationMcpToolsetTestSourceTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()
    private lateinit var testSourceRoot: VirtualFile

    override fun setUp() {
        super.setUp()
        testSourceRoot = myFixture.tempDirFixture.findOrCreateDir("src/test/kotlin")
        PsiTestUtil.addSourceRoot(module, testSourceRoot, true)
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        myFixture.copyDirectoryToProject("springBootApp", "")
        addProductionExchangeClient()
        addProbeControllerToTests()
    }

    override fun tearDown() {
        try {
            PsiTestUtil.removeSourceRoot(module, testSourceRoot)
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testFindListsTheProductionHandlerBeforeTheTestProbe() = runBlocking<Unit> {
        assertOnlyTheProbeIsDeclaredInTestSources()

        val endpoints = find("/api/routes/7")["endpoints"]

        assertEquals(
            "The probe's longer template outranks the production route by specificity, and must still follow it",
            listOf(ROUTE_BY_ID, PROBE_ROUTE),
            paths(endpoints)
        )
        assertFalse("A production endpoint carries no 'testSource' key, got ${endpoints[0]}", endpoints[0].has("testSource"))
        assertTrue("The probe is marked as test code, got ${endpoints[1]}", endpoints[1].path("testSource").asBoolean())
    }

    fun testListingShowsTheTestProbeLastAndFlagged() = runBlocking<Unit> {
        assertOnlyTheProbeIsDeclaredInTestSources()

        val endpoints = list(compact = true)["endpoints"].toList()
        val classes = endpoints.map { it["controllerClass"].asText() }

        assertTrue("The production exchange client must be listed, got $classes", EXCHANGE_CLIENT_CLASS in classes)
        assertEquals("The probe is listed by an earlier loader than the exchange client", PROBE_CLASS, classes.last())
        assertTrue("The probe is marked as test code, got ${endpoints.last()}", endpoints.last().path("testSource").asBoolean())
        assertEquals(COMPACT_KEYS + "testSource", endpoints.last().fieldNames().asSequence().toSet())

        val export = endpoints.single { it["methodName"].asText() == "export" }
        assertEquals("A production record keeps its exact shape", COMPACT_KEYS, export.fieldNames().asSequence().toSet())
        assertTrue(
            "No production endpoint carries 'testSource', got ${endpoints.dropLast(1)}",
            endpoints.dropLast(1).none { it.has("testSource") }
        )
    }

    fun testContractOfASharedRouteListsProductionFirstAndFlagsTheProbe() = runBlocking<Unit> {
        assertOnlyTheProbeIsDeclaredInTestSources()

        val endpoints = contract("/api/routes/7")["endpoints"].toList()

        assertEquals(listOf(ROUTE_CONTROLLER_CLASS, PROBE_CLASS), endpoints.map { it["controllerClass"].asText() })
        assertEquals(listOf("COMPLETE", "COMPLETE"), endpoints.map { it["contractStatus"].asText() })
        assertFalse("A production contract carries no 'testSource' key, got ${endpoints[0]}", endpoints[0].has("testSource"))
        assertTrue("The probe's contract is marked as test code, got ${endpoints[1]}", endpoints[1].path("testSource").asBoolean())
    }

    fun testTestProbeLandsOnTheLastPage() = runBlocking<Unit> {
        assertOnlyTheProbeIsDeclaredInTestSources()
        val total = list(compact = true)["totalCount"].asInt()
        assertTrue("The fixture needs at least two endpoints to split into pages, got $total", total >= 2)

        val firstPage = list(compact = true, limit = total - 1)
        val lastPage = list(compact = true, offset = total - 1, limit = 1)

        assertTrue(firstPage["truncated"].asBoolean())
        assertEquals(total - 1, firstPage["endpoints"].size())
        assertFalse(
            "The probe belongs on the last page, got ${classesOf(firstPage["endpoints"])}",
            PROBE_CLASS in classesOf(firstPage["endpoints"])
        )
        assertTrue(firstPage["endpoints"].none { it.has("testSource") })
        assertEquals(listOf(PROBE_CLASS), classesOf(lastPage["endpoints"]))
        assertTrue(lastPage["endpoints"].single().path("testSource").asBoolean())
        assertFalse(lastPage["truncated"].asBoolean())
    }

    fun testNeighbourhoodOfAMissListsProductionRoutesBeforeTheTestProbe() = runBlocking<Unit> {
        assertOnlyTheProbeIsDeclaredInTestSources()

        val root = find("/api/routes/export/preview")
        val nearest = root["nearestByPrefix"].toList()

        assertEquals(0, root["totalCount"].asInt())
        assertEquals(listOf("/api/routes/export", ROUTE_BY_ID, PROBE_ROUTE), paths(nearest))
        assertTrue("No production neighbour carries 'testSource', got $nearest", nearest.dropLast(1).none { it.has("testSource") })
        assertTrue("The probe neighbour is marked as test code, got ${nearest.last()}", nearest.last().path("testSource").asBoolean())
    }

    fun testProbeInheritingAProductionMappingIsStillMarkedAndRankedLast() = runBlocking<Unit> {
        addProductionBaseWithProductionAndTestSubclasses()
        val fileIndex = ProjectFileIndex.getInstance(project)
        assertTrue("The probe subclass must live in a test source root", fileIndex.isInTestSourceContent(fileOf(PROBE_VIA_BASE_CLASS)))
        assertFalse("The base declaring the mapping must live in production", fileIndex.isInTestSourceContent(fileOf(BASE_CONTROLLER_CLASS)))
        assertFalse("The production subclass must live in production", fileIndex.isInTestSourceContent(fileOf(ROUTE_VIA_BASE_CLASS)))

        val found = find("/api/base/1")["endpoints"].toList()
        assertEquals(
            "Both subclasses serve the inherited mapping, the production one first",
            listOf(ROUTE_VIA_BASE_CLASS, PROBE_VIA_BASE_CLASS),
            classesOf(found)
        )
        assertEquals("The mapping itself is declared in production", listOf("get", "get"), found.map { it["methodName"].asText() })
        assertFalse("A production endpoint carries no 'testSource' key, got ${found[0]}", found[0].has("testSource"))
        assertTrue("The probe is test code although its mapping is declared in production, got ${found[1]}", found[1].path("testSource").asBoolean())

        val classes = classesOf(list(compact = true)["endpoints"])
        assertTrue("The production exchange client must be listed, got $classes", EXCHANGE_CLIENT_CLASS in classes)
        assertEquals(
            "Both probes follow every production endpoint, the exchange client included",
            setOf(PROBE_CLASS, PROBE_VIA_BASE_CLASS),
            classes.takeLast(2).toSet()
        )
    }

    private fun assertOnlyTheProbeIsDeclaredInTestSources() {
        val fileIndex = ProjectFileIndex.getInstance(project)
        assertTrue("The probe must live in a test source root", fileIndex.isInTestSourceContent(fileOf(PROBE_TEST_CLASS)))
        assertFalse("The controller must live in production", fileIndex.isInTestSourceContent(fileOf(ROUTE_CONTROLLER_CLASS)))
        assertFalse("The exchange client must live in production", fileIndex.isInTestSourceContent(fileOf(EXCHANGE_CLIENT_CLASS)))
    }

    private fun fileOf(className: String): VirtualFile = myFixture.findClass(className).containingFile.virtualFile

    private suspend fun find(url: String): JsonNode =
        mapper.readTree(toolset.findEndpoint(urlPattern = url, projectPath = project.basePath, httpMethod = ""))

    private suspend fun contract(url: String): JsonNode =
        mapper.readTree(toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = ""))

    private suspend fun list(compact: Boolean, offset: Int = 0, limit: Int = 500): JsonNode =
        mapper.readTree(
            toolset.getHttpEndpoints(projectPath = project.basePath, offset = offset, limit = limit, compact = compact)
        )

    private fun paths(nodes: Iterable<JsonNode>): List<String> = nodes.map { it["fullPath"].asText() }

    private fun classesOf(nodes: Iterable<JsonNode>): List<String> = nodes.map { it["controllerClass"].asText() }

    private fun addProductionExchangeClient() {
        myFixture.addFileToProject(
            "com/example/app/client/RouteClient.java", """
            package com.example.app.client;

            import org.springframework.web.service.annotation.GetExchange;
            import org.springframework.web.service.annotation.HttpExchange;

            @HttpExchange("/remote")
            public interface RouteClient {
                @GetExchange("/routes")
                String routes();
            }
            """.trimIndent()
        )
    }

    private fun addProductionBaseWithProductionAndTestSubclasses() {
        myFixture.addFileToProject(
            "com/example/app/web/BaseRouteController.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RestController

            open class BaseRouteController {
                @GetMapping("/api/base/{id}")
                fun get(@PathVariable id: String): String = id
            }

            @RestController
            class RouteViaBase : BaseRouteController()
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/test/kotlin/com/example/app/web/ProbeViaBase.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.RestController

            @RestController
            class ProbeViaBase : BaseRouteController()
            """.trimIndent()
        )
    }

    private fun addProbeControllerToTests() {
        myFixture.addFileToProject(
            "src/test/kotlin/com/example/app/web/RouteControllerTest.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RestController

            class RouteControllerTest {

                @RestController
                class ProbeController {
                    @GetMapping("/api/routes/{routeId}")
                    fun probe(@PathVariable routeId: String): String = routeId
                }
            }
            """.trimIndent()
        )
    }

    private companion object {
        const val ROUTE_CONTROLLER_CLASS = "com.example.app.web.RouteController"
        const val EXCHANGE_CLIENT_CLASS = "com.example.app.client.RouteClient"
        const val PROBE_TEST_CLASS = "com.example.app.web.RouteControllerTest"
        const val PROBE_CLASS = "$PROBE_TEST_CLASS.ProbeController"
        const val BASE_CONTROLLER_CLASS = "com.example.app.web.BaseRouteController"

        const val ROUTE_VIA_BASE_CLASS = "com.example.app.web.RouteViaBase"
        const val PROBE_VIA_BASE_CLASS = "com.example.app.web.ProbeViaBase"
        const val ROUTE_BY_ID = "/api/routes/{id}"
        const val PROBE_ROUTE = "/api/routes/{routeId}"
        val COMPACT_KEYS = setOf("httpMethods", "fullPath", "controllerClass", "methodName", "filePath", "line", "endpointType")
    }
}
