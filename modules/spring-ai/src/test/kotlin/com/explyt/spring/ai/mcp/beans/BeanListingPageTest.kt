/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.ai.mcp.SpringBootApplicationMcpToolset
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpExpectedError
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

class BeanListingPageTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.jakarta_persistence_3_1_0,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testListingIsAnEnvelope() = runBlocking<Unit> {
        copyDemoApplication()

        val page = listing(DEMO_APPLICATION, "CONTROLLER")

        assertTrue("The listing must be an object, got $page", page.isObject)
        assertEquals("OK", page["status"]?.asText())
        assertTrue("Rows are served under 'beans', got $page", page["beans"]?.isArray == true)
        assertTrue("A listing names its revision, got $page", page["revision"]?.asText().orEmpty().isNotEmpty())
        assertEquals(page["beans"].size(), page["totalCount"]?.asInt())
        assertEquals(0, page["offset"]?.asInt())
    }

    fun testProjectBeanIsMarkedProject() = runBlocking<Unit> {
        copyDemoApplication()
        assertResolves("com.example.app.service.DemoService")

        val service = rows(listing(DEMO_APPLICATION, "COMPONENT"))
            .firstOrNull { it["className"].asText() == "com.example.app.service.DemoService" }

        assertNotNull("Precondition: DemoService is listed as a component", service)
        assertEquals("PROJECT", service!!["origin"]?.asText())
    }

    fun testLibraryTypedFactoryBeanIsProject() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("beanQuery", "")
        assertResolves("com.explyt.demo.TimeConfig")

        val clock = rows(listing(BEAN_QUERY_APPLICATION, "COMPONENT"))
            .firstOrNull { it["beanName"].asText() == "systemClock" }

        assertNotNull("Precondition: systemClock is listed", clock)
        assertEquals("java.time.Clock", clock!!["className"].asText())
        assertEquals(
            "The factory is declared in the project although its type is the JDK's",
            "PROJECT", clock["origin"]?.asText()
        )
    }

    fun testLibraryBeanIsMarkedLibrary() = runBlocking<Unit> {
        copyDemoApplication()
        assertTrue(
            "Precondition: $LIBRARY_CONTROLLER comes from a library jar",
            JavaPsiFacade.getInstance(project).findClass(LIBRARY_CONTROLLER, GlobalSearchScope.allScope(project))
                ?.containingFile?.virtualFile?.path.orEmpty().contains(".jar!/")
        )

        val controller = rows(listing(DEMO_APPLICATION, "CONTROLLER"))
            .firstOrNull { it["className"].asText() == LIBRARY_CONTROLLER }

        assertNotNull("Precondition: basicErrorController is listed", controller)
        assertEquals("LIBRARY", controller!!["origin"]?.asText())
    }

    fun testOriginFilterProject() = runBlocking<Unit> {
        copyDemoApplication()
        val everything = listing(DEMO_APPLICATION, "CONTROLLER", limit = 50, maxChars = 16000)
        assertTrue(
            "Precondition: the fixture has library controllers to filter out, got $everything",
            rows(everything).any { it["origin"]?.asText() == "LIBRARY" }
        )
        assertTrue(
            "Precondition: every fixture row has a known origin, got $everything",
            rows(everything).all { it.has("origin") }
        )

        val page = listing(DEMO_APPLICATION, "CONTROLLER", origin = "project", limit = 50, maxChars = 16000)

        assertEquals("OK", page["status"]?.asText())
        assertEquals(
            setOf("demoController", "resolverController", "routeController"),
            rows(page).map { it["beanName"].asText() }.toSet()
        )
        assertTrue("Every row is PROJECT, got $page", rows(page).all { it["origin"]?.asText() == "PROJECT" })
        assertEquals(3, page["totalCount"]?.asInt())
        assertFalse("No row lacks an origin here, so nothing is counted as unknown: $page", page.has("unknownOriginCount"))
    }

    fun testUnknownOriginIsRejectedWithTheValidValues() = runBlocking<Unit> {
        copyDemoApplication()

        val error = rejected { listingText(DEMO_APPLICATION, "CONTROLLER", origin = "MINE") }

        assertEquals("Unknown origin 'MINE'. Valid values: PROJECT, LIBRARY.", error)
    }

    fun testPagingContinuation() = runBlocking<Unit> {
        copyDemoApplication()
        val unpaged = listing(DEMO_APPLICATION, "CONTROLLER", limit = 50, maxChars = 16000)
        val total = unpaged["totalCount"]?.asInt() ?: -1
        assertTrue("Precondition: more than one controller to page through, got $unpaged", total > 1)

        val first = listing(DEMO_APPLICATION, "CONTROLLER", limit = 1)
        assertEquals("OK", first["status"]?.asText())
        assertTrue("A one-row page of $total is truncated, got $first", first["truncated"]?.asBoolean() == true)
        assertEquals(1, first["nextOffset"]?.asInt())

        val revision = first["revision"].asText()
        val served = rows(first).toMutableList()
        var page = first
        while (page["truncated"]?.asBoolean() == true) {
            page = listing(
                DEMO_APPLICATION, "CONTROLLER",
                offset = page["nextOffset"].asInt(), limit = 1, expectedRevision = revision
            )
            assertEquals("OK", page["status"]?.asText())
            assertEquals(revision, page["revision"]?.asText())
            served += rows(page)
        }

        val keys = served.map(::rowKey)
        assertEquals("No row repeats across pages: $keys", keys.size, keys.toSet().size)
        assertEquals(rows(unpaged).map(::rowKey), keys)
    }

    fun testRevisionChangesWithOrigin() = runBlocking<Unit> {
        copyDemoApplication()

        val all = listing(DEMO_APPLICATION, "CONTROLLER")
        val projectOnly = listing(DEMO_APPLICATION, "CONTROLLER", origin = "PROJECT")

        assertEquals("OK", all["status"]?.asText())
        assertEquals("OK", projectOnly["status"]?.asText())
        assertFalse(
            "Another origin filter is another answer and must carry another revision",
            all["revision"].asText() == projectOnly["revision"].asText()
        )
    }

    fun testStaleRevisionIsResultChanged() = runBlocking<Unit> {
        copyDemoApplication()

        val page = listing(DEMO_APPLICATION, "CONTROLLER", offset = 1, limit = 1, expectedRevision = "stale")

        assertEquals("ERROR", page["status"]?.asText())
        assertEquals("RESULT_CHANGED", page["error"]?.get("code")?.asText())
    }

    fun testOrderProjectFirst() = runBlocking<Unit> {
        copyDemoApplication()

        val beans = rows(listing(DEMO_APPLICATION, "CONTROLLER", limit = 50, maxChars = 16000))
        assertTrue(
            "Precondition: both origins are present, got $beans",
            beans.map { it["origin"]?.asText() }.containsAll(listOf("PROJECT", "LIBRARY"))
        )

        val expected = beans.sortedWith(
            compareBy<JsonNode>(
                { ORIGIN_ORDER.indexOf(it["origin"]?.asText()) },
                { it["beanName"].asText() },
                { it["className"].asText() }
            )
        )
        assertEquals(expected.map(::rowKey), beans.map(::rowKey))
    }

    private fun copyDemoApplication() {
        myFixture.copyDirectoryToProject("springBootApp", "")
        assertResolves(DEMO_APPLICATION)
    }

    private fun assertResolves(className: String) {
        assertNotNull(
            "Precondition: $className resolves in the fixture",
            JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.projectScope(project))
        )
    }

    private fun rows(page: JsonNode): List<JsonNode> {
        assertEquals("Expected an OK envelope, got $page", "OK", page["status"]?.asText())
        val beans = page["beans"]
        assertTrue("Expected a 'beans' array, got $page", beans?.isArray == true)
        return beans.toList()
    }

    private fun rowKey(row: JsonNode): String = row["beanName"].asText() + "|" + row["className"].asText()

    private suspend fun listing(
        application: String,
        beanType: String,
        origin: String? = null,
        offset: Int? = null,
        limit: Int? = null,
        maxChars: Int? = null,
        expectedRevision: String? = null,
    ): JsonNode = mapper.readTree(
        listingText(application, beanType, origin, offset, limit, maxChars, expectedRevision)
    )

    private suspend fun listingText(
        application: String,
        beanType: String,
        origin: String? = null,
        offset: Int? = null,
        limit: Int? = null,
        maxChars: Int? = null,
        expectedRevision: String? = null,
    ): String {
        val arguments = mapOf(
            "applicationClassName" to application,
            "projectPath" to project.basePath,
            "beanType" to beanType,
            "source" to "STATIC",
            "origin" to origin,
            "offset" to offset,
            "limit" to limit,
            "maxChars" to maxChars,
            "expectedRevision" to expectedRevision,
        ).filterValues { it != null }
        val function = SpringBootApplicationMcpToolset::class.functions.single { it.name == "applicationBeans" }
        val parameters = function.parameters.associateBy { it.name }
        val missing = arguments.keys - parameters.keys
        assertTrue("applicationBeans takes no ${missing.joinToString()} argument", missing.isEmpty())
        val bound = arguments.mapKeys { parameters.getValue(it.key) } + (function.instanceParameter!! to toolset)
        return try {
            function.callSuspendBy(bound) as String
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private suspend fun rejected(call: suspend () -> String): String {
        try {
            val answer = call()
            fail("Expected the argument to be rejected, got $answer")
            error("unreachable")
        } catch (error: McpExpectedError) {
            return error.message.orEmpty()
        }
    }

    private companion object {
        const val DEMO_APPLICATION = "com.example.app.DemoApplication"
        const val BEAN_QUERY_APPLICATION = "com.explyt.demo.App"
        const val LIBRARY_CONTROLLER = "org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController"
        val ORIGIN_ORDER = listOf("PROJECT", "LIBRARY", null)
    }
}
