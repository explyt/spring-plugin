/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking

/**
 * The built-in Actuator endpoints as the MCP endpoint tools report them: found by the URL a health check calls, and
 * marked with whether the configuration exposes them over HTTP rather than hidden when it does not.
 */
class SpringBootApplicationMcpToolsetActuatorTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
        TestLibrary.springWebMvc_6_0_7,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: Boot's HealthEndpoint is on the classpath",
            facade.findClass(HEALTH_ENDPOINT, scope)
        )
        assertNotNull(
            "precondition: Boot's endpoint auto-configuration is on the classpath",
            facade.findClass(
                "org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration", scope
            )
        )
        myFixture.addFileToProject(
            "com/example/app/DemoApplication.kt", """
            package com.example.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class DemoApplication
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/web/OrdersController.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class OrdersController {
                @GetMapping("/api/orders")
                fun orders(): List<String> = emptyList()
            }
            """.trimIndent()
        )
    }

    /**
     * `health` is two operations - `/actuator/health` and `/actuator/health/{path}` for one component - and the lookup
     * lists the exact route first, the way it orders any other match.
     */
    fun testHealthIsFoundAndExposedByDefault() = runBlocking<Unit> {
        val found = find("/actuator/health")

        assertEquals(listOf("/actuator/health", "/actuator/health/{path}"), found.map { it["fullPath"].asText() })
        val health = found.first()
        assertEquals("Actuator", health["endpointType"].asText())
        assertEquals("health", health["methodName"].asText())
        assertEquals("EXPOSED", health["exposed"].asText())
    }

    fun testInfoIsFoundButNotExposedByDefault() = runBlocking<Unit> {
        assertEquals("NOT_EXPOSED", exactMatch("/actuator/info")["exposed"].asText())
    }

    fun testIncludeListExposesTheListedEndpoints() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "application.yaml", """
            management:
              endpoints:
                web:
                  exposure:
                    include: health,info
            """.trimIndent()
        )

        assertEquals("EXPOSED", exactMatch("/actuator/info")["exposed"].asText())
        assertEquals("NOT_EXPOSED", exactMatch("/actuator/env")["exposed"].asText())
    }

    /**
     * A deployment health check calls the URL under the servlet context path, which no Actuator path declares. The
     * endpoint is declared in a jar, so the module whose base path applies is found through the jar's file.
     */
    fun testHealthUnderTheDeclaredContextPathIsFoundWithItsBasePath() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "application.yaml", """
            server:
              servlet:
                context-path: /shop
            """.trimIndent()
        )

        val root = mapper.readTree(toolset.findEndpoint(urlPattern = "/shop/actuator/health", projectPath = projectPath()))

        assertEquals("a declared base path, not a guess", "/shop", root["basePath"].asText())
        assertTrue(root["assumedPrefix"].isNull)
        assertEquals(listOf("/actuator/health"), root["endpoints"].map { it["fullPath"].asText() })
        assertEquals("EXPOSED", root["endpoints"].single()["exposed"].asText())
    }

    fun testListingOfActuatorEndpointsCarriesExposureOnEveryRecord() = runBlocking<Unit> {
        val listed = listActuator(compact = false)

        val paths = listed.map { it["fullPath"].asText() }
        assertTrue("health is listed, got $paths", "/actuator/health" in paths)
        assertTrue("info is listed although not exposed, got $paths", "/actuator/info" in paths)
        assertTrue(
            "every Actuator record carries 'exposed', got ${listed.filter { !it.has("exposed") }}",
            listed.all { it.has("exposed") }
        )
        assertEquals(
            "the compact listing carries the same fact",
            listed.associate { it["fullPath"].asText() to it["exposed"].asText() },
            listActuator(compact = true).associate { it["fullPath"].asText() to it["exposed"].asText() }
        )
    }

    /** `exposed` is a fact about Actuator endpoints; any other endpoint keeps its documented shape. */
    fun testControllerEndpointHasNoExposedKey() = runBlocking<Unit> {
        val listed = mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), controllerFilter = "OrdersController")
        )["endpoints"].single()

        assertEquals(
            setOf(
                "httpMethods", "fullPath", "controllerClass", "methodName",
                "filePath", "line", "parameters", "returnType", "endpointType",
            ),
            listed.fieldNames().asSequence().toSet()
        )
        assertFalse(find("/api/orders").single().has("exposed"))
    }

    fun testContractOfHealthCarriesExposure() = runBlocking<Unit> {
        val contract = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/actuator/health", projectPath = projectPath())
        )["endpoints"].first()

        assertEquals("/actuator/health", contract["fullPath"].asText())
        assertEquals("EXPOSED", contract["exposed"].asText())
        assertFalse(
            "a controller contract has no 'exposed' key",
            mapper.readTree(toolset.getEndpointContract(urlPattern = "/api/orders", projectPath = projectPath()))
                ["endpoints"].single().has("exposed")
        )
    }

    /**
     * A built-in endpoint is declared in a jar under the user's home, which is nothing an agent can open and a path
     * that must not leave the machine: the record names the jar instead, and keeps the class as its identity.
     */
    fun testLibraryEndpointIsLocatedByItsJarNotByAnAbsolutePath() = runBlocking<Unit> {
        val healthEndpoint = JavaPsiFacade.getInstance(project)
            .findClass(HEALTH_ENDPOINT, GlobalSearchScope.allScope(project))!!
        val expectedJar = JarFileSystem.getInstance()
            .getVirtualFileForJar(healthEndpoint.containingFile.virtualFile)?.name
        assertNotNull("precondition: HealthEndpoint is read from a jar", expectedJar)

        val listed = listActuator(compact = false).single { it["fullPath"].asText() == "/actuator/health" }
        val contract = contractOf("/actuator/health", "GET")

        for (record in listOf(listed, contract)) {
            assertNoLocalPath(record)
            assertTrue("filePath is unknown for a library element, got ${record["filePath"]}", record["filePath"].isNull)
            assertEquals(expectedJar, record["library"].asText())
            assertEquals(HEALTH_ENDPOINT, record["controllerClass"].asText())
        }
    }

    fun testProjectEndpointKeepsItsFilePathAndNamesNoLibrary() = runBlocking<Unit> {
        val orders = find("/api/orders").single()

        assertEquals("com/example/app/web/OrdersController.kt", orders["filePath"].asText())
        assertFalse("a project element names no library, got $orders", orders.has("library"))
        assertNoLocalPath(orders)
    }

    /** Access is a gate independent of exposure: `shutdown` is exposed by `*` yet answers 404 without access. */
    fun testAccessIsReportedNextToExposure() = runBlocking<Unit> {
        myFixture.addFileToProject("application.properties", "management.endpoints.web.exposure.include=*")

        val shutdown = exactMatch("/actuator/shutdown")
        assertEquals("EXPOSED", shutdown["exposed"].asText())
        assertEquals("NONE", shutdown["access"].asText())
        assertEquals("UNRESTRICTED", exactMatch("/actuator/health")["access"].asText())
        assertEquals(
            "the listing carries the same fact",
            "NONE",
            listActuator(compact = true).single { it["fullPath"].asText() == "/actuator/shutdown" }["access"].asText()
        )
        assertEquals(
            "the contract carries the same fact",
            "NONE",
            contractOf("/actuator/shutdown", "POST")["access"].asText()
        )
        assertFalse("a controller endpoint has no 'access' key", find("/api/orders").single().has("access"))
    }

    fun testReadOnlyAccessDeniesAWriteOperation() = runBlocking<Unit> {
        myFixture.addFileToProject("application.properties", "management.endpoints.access.default=read-only")

        val loggers = find("/actuator/loggers/{name}").filter { it["fullPath"].asText() == "/actuator/loggers/{name}" }
        val accessByVerb = loggers.associate { it["httpMethods"].single().asText() to it["access"].asText() }
        assertEquals(mapOf("GET" to "READ_ONLY", "POST" to "NONE"), accessByVerb)
    }

    /** `@ReadOperation(produces = ...)` reaches the record: the thread dump also answers as plain text. */
    fun testOperationProducesIsReported() = runBlocking<Unit> {
        val produces = find("/actuator/threaddump")
            .filter { it["fullPath"].asText() == "/actuator/threaddump" }
            .flatMap { record -> record["produces"]?.map { it.asText() }.orEmpty() }

        assertTrue("the text dump's media type is reported, got $produces", produces.any { it.startsWith("text/plain") })
    }

    /** A `@Selector` is a path segment, and a write operation's other arguments are fields of its JSON body. */
    fun testSelectorIsAPathParameterAndWriteArgumentsAreTheBody() = runBlocking<Unit> {
        val write = contractOf("/actuator/loggers/{name}", "POST")["parameters"].associateBy { it["name"].asText() }
        assertEquals("PATH", write.getValue("name")["source"].asText())
        assertEquals("BODY", write.getValue("configuredLevel")["source"].asText())
        assertFalse("a @Nullable argument is optional", write.getValue("configuredLevel")["required"].asBoolean())

        val read = contractOf("/actuator/loggers/{name}", "GET")["parameters"].associateBy { it["name"].asText() }
        assertEquals("PATH", read.getValue("name")["source"].asText())
    }

    private suspend fun contractOf(url: String, method: String): JsonNode =
        mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = projectPath(), httpMethod = method)
        )["endpoints"].single { it["fullPath"].asText() == url }

    private fun projectPath(): String = project.basePath ?: ""

    private suspend fun find(url: String): List<JsonNode> =
        mapper.readTree(toolset.findEndpoint(urlPattern = url, projectPath = projectPath()))["endpoints"].toList()

    private suspend fun exactMatch(url: String): JsonNode = find(url).single { it["fullPath"].asText() == url }

    private suspend fun listActuator(compact: Boolean): List<JsonNode> =
        mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), endpointType = "ACTUATOR", compact = compact)
        )["endpoints"].toList()

    private fun assertNoLocalPath(node: JsonNode) {
        val home = System.getProperty("user.home")
        val homeForms = listOf(home, home.replace('/', '\\'))
        val leaks = textFieldsOf(node).filter { (key, value) ->
            homeForms.any(value::contains) || value.contains("jar://") || value.contains("file://") ||
                    value.contains(".jar!/") || (isLocationKey(key) && !isPortable(value))
        }
        assertTrue("no answer may carry a local path, got $leaks", leaks.isEmpty())
    }

    private fun isLocationKey(key: String?): Boolean =
        key != null && key !in URL_PATH_KEYS && (key == "library" || key.endsWith("Path") || key.endsWith("Url"))

    private fun isPortable(value: String): Boolean =
        !value.startsWith("/") && !value.startsWith("\\") && !value.matches(Regex("^[A-Za-z]:.*")) &&
                !value.contains("!/") && !value.contains("://")

    private fun textFieldsOf(node: JsonNode, key: String? = null): List<Pair<String?, String>> = when {
        node.isTextual -> listOf(key to node.asText())
        node.isObject -> node.fields().asSequence().flatMap { (name, value) -> textFieldsOf(value, name) }.toList()
        node.isArray -> node.flatMap { textFieldsOf(it, key) }
        else -> emptyList()
    }

    private companion object {
        const val HEALTH_ENDPOINT = "org.springframework.boot.health.actuate.endpoint.HealthEndpoint"
        val URL_PATH_KEYS = setOf("fullPath", "basePath", "endpointPath")
    }
}
