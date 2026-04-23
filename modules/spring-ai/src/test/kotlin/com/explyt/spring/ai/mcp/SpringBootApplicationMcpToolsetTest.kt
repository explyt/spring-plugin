/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

class SpringBootApplicationMcpToolsetTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.jakarta_persistence_3_1_0,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    private fun projectPath(): String = project.basePath ?: ""

    private fun parseArray(json: String): JsonNode = mapper.readTree(json)

    private fun texts(node: JsonNode, field: String): List<String> =
        node.mapNotNull { it[field]?.asText() }

    fun testGetAllSpringBootApplications() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getAllSpringBootApplications(projectPath())
        val applications = parseArray(result)

        assertEquals(1, applications.size())
        assertEquals("com.example.app.DemoApplication", applications[0]["fullyQualifiedClassName"].asText())
    }

    fun testApplicationBeansComponent() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.applicationBeans(
            applicationClassName = "com.example.app.DemoApplication",
            projectPath = projectPath(),
            beanType = "COMPONENT"
        )
        val classNames = texts(parseArray(result), "className")
        assertTrue(
            "Expected DemoService as COMPONENT bean, got $classNames",
            classNames.contains("com.example.app.service.DemoService")
        )
    }

    fun testApplicationBeansController() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.applicationBeans(
            applicationClassName = "com.example.app.DemoApplication",
            projectPath = projectPath(),
            beanType = "CONTROLLER"
        )
        val classNames = texts(parseArray(result), "className")
        assertTrue(
            "Expected DemoController as CONTROLLER bean, got $classNames",
            classNames.contains("com.example.app.web.DemoController")
        )
    }

    fun testApplicationBeansRepository() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.applicationBeans(
            applicationClassName = "com.example.app.DemoApplication",
            projectPath = projectPath(),
            beanType = "REPOSITORY"
        )
        val classNames = texts(parseArray(result), "className")
        assertTrue(
            "Expected DemoRepository as REPOSITORY bean, got $classNames",
            classNames.contains("com.example.app.repository.DemoRepository")
        )
    }

    fun testApplicationBeansInvalidBeanTypeFails() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        try {
            toolset.applicationBeans(
                applicationClassName = "com.example.app.DemoApplication",
                projectPath = projectPath(),
                beanType = "NOT_A_REAL_TYPE"
            )
            fail("Expected failure for unknown bean type")
        } catch (_: Exception) {
            // expected: mcpFail throws for unknown bean type
        }
    }

    fun testFindEndpoint() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.findEndpoint(
            urlPattern = "/api/demo/items/{id}",
            projectPath = projectPath(),
            httpMethod = "GET"
        )
        val endpoints = parseArray(result)

        assertEquals(1, endpoints.size())
        val endpoint = endpoints[0]
        assertEquals("/api/demo/items/{id}", endpoint["fullPath"].asText())
        assertEquals("com.example.app.web.DemoController", endpoint["controllerClass"].asText())
        assertEquals("getItem", endpoint["methodName"].asText())
        val httpMethods = endpoint["httpMethods"].map { it.asText() }
        assertTrue("Expected GET in $httpMethods", httpMethods.contains("GET"))
    }

    fun testFindEndpointNoMatch() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.findEndpoint(
            urlPattern = "/does/not/exist/anywhere",
            projectPath = projectPath(),
            httpMethod = ""
        )
        val endpoints = parseArray(result)
        assertEquals("Expected no matching endpoints, got $endpoints", 0, endpoints.size())
    }

    fun testGetHttpEndpoints() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "DemoController",
            endpointType = ""
        )
        val methodNames = texts(parseArray(result), "methodName").toSet()
        assertTrue("Expected getItem in $methodNames", methodNames.contains("getItem"))
        assertTrue("Expected createItem in $methodNames", methodNames.contains("createItem"))
    }

    fun testGetEndpointContract() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getEndpointContract(
            urlPattern = "/api/demo/items/{id}",
            projectPath = projectPath(),
            httpMethod = "GET"
        )
        val contracts = parseArray(result)
        assertEquals(1, contracts.size())
        val contract = contracts[0]
        assertEquals("/api/demo/items/{id}", contract["fullPath"].asText())
        assertEquals("getItem", contract["methodName"].asText())

        val produces = contract["produces"].map { it.asText() }
        assertTrue("Expected 'application/json' in produces=$produces", produces.contains("application/json"))

        val parameters = contract["parameters"]
        val pathParam = parameters.firstOrNull { it["source"].asText() == "PATH" }
        assertNotNull("Expected PATH parameter in $parameters", pathParam)
        assertEquals("id", pathParam!!["name"].asText())
    }

    fun testTraceCallChainFileNotFoundFails() = runBlocking<Unit> {
        // `traceCallChain` resolves files via `LocalFileSystem` using `project.basePath + filePath`.
        // In a light test fixture, testdata lives in an in-memory temp VFS, so any real path
        // lookup must fail. This covers the explicit file-not-found error branch.
        myFixture.copyDirectoryToProject("springBootApp", "")

        try {
            toolset.traceCallChain(
                filePath = "does/not/exist/DemoController.java",
                line = 1,
                projectPath = projectPath(),
                depth = 1,
                includeTests = false
            )
            fail("Expected failure when file path cannot be resolved")
        } catch (_: Exception) {
            // expected: mcpFail("file not found: ...")
        }
    }

    fun testGetSpringDataEntities() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getSpringDataEntities(
            projectPath = projectPath(),
            packageFilter = "com.example.app"
        )
        val entities = parseArray(result)
        assertEquals(1, entities.size())
        val entity = entities[0]
        assertEquals("DemoEntity", entity["name"].asText())
        assertEquals("com.example.app.entity.DemoEntity", entity["className"].asText())
        assertEquals("demo_table", entity["tableName"].asText())

        val fields = entity["fields"]
        val idField = fields.firstOrNull { it["name"].asText() == "id" }
        assertNotNull("Expected id field in $fields", idField)
        assertEquals(true, idField!!["primaryKey"].asBoolean())
        assertEquals("id", idField["column"].asText())

        val titleField = fields.firstOrNull { it["name"].asText() == "title" }
        assertNotNull("Expected title field in $fields", titleField)
        assertEquals("title", titleField!!["column"].asText())
        assertEquals(false, titleField["nullable"].asBoolean())
    }

    // ---- additional coverage for uncovered branches ----

    fun testApplicationBeansMissingApplicationClassFails() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        try {
            toolset.applicationBeans(
                applicationClassName = "com.example.app.NoSuchApplication",
                projectPath = projectPath(),
                beanType = "COMPONENT"
            )
            fail("Expected failure when application class is not found")
        } catch (_: Exception) {
            // expected: mcpFail("Spring Boot Application class not found ...")
        }
    }

    fun testFindEndpointBlankUrlPatternFails() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        try {
            toolset.findEndpoint(
                urlPattern = "   ",
                projectPath = projectPath(),
                httpMethod = ""
            )
            fail("Expected failure when urlPattern is blank")
        } catch (_: Exception) {
            // expected: mcpFail("urlPattern must not be empty")
        }
    }

    fun testFindEndpointPartialUrlMatch() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.findEndpoint(
            urlPattern = "items",
            projectPath = projectPath(),
            httpMethod = ""
        )
        val methodNames = texts(parseArray(result), "methodName").toSet()
        assertTrue(
            "Expected partial-url match to return getItem + createItem, got $methodNames",
            methodNames.containsAll(setOf("getItem", "createItem"))
        )
    }

    fun testFindEndpointHttpMethodFilterExcludes() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.findEndpoint(
            urlPattern = "/api/demo/items",
            projectPath = projectPath(),
            httpMethod = "PUT"
        )
        val endpoints = parseArray(result)
        assertEquals(
            "Expected no endpoints for PUT filter on GET/POST endpoints, got $endpoints",
            0, endpoints.size()
        )
    }

    fun testGetHttpEndpointsEndpointTypeFilter() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val mvcResult = toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "",
            endpointType = "SPRING_MVC"
        )
        val mvcMethodNames = texts(parseArray(mvcResult), "methodName").toSet()
        assertTrue(
            "Expected Spring MVC endpoints (getItem, createItem), got $mvcMethodNames",
            mvcMethodNames.containsAll(setOf("getItem", "createItem"))
        )

        val feignResult = toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "",
            endpointType = "SPRING_OPEN_FEIGN"
        )
        val feignEndpoints = parseArray(feignResult)
        assertEquals(
            "Expected no Feign endpoints in test fixture, got $feignEndpoints",
            0, feignEndpoints.size()
        )
    }

    fun testGetHttpEndpointsControllerFilterNoMatch() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "NoSuchControllerAnywhere",
            endpointType = ""
        )
        val endpoints = parseArray(result)
        assertEquals(
            "Expected no endpoints when controllerFilter matches nothing, got $endpoints",
            0, endpoints.size()
        )
    }

    fun testGetEndpointContractBlankUrlPatternFails() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        try {
            toolset.getEndpointContract(
                urlPattern = "",
                projectPath = projectPath(),
                httpMethod = ""
            )
            fail("Expected failure when urlPattern is blank")
        } catch (_: Exception) {
            // expected: mcpFail("urlPattern must not be empty")
        }
    }

    fun testGetEndpointContractPostWithRequestBody() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getEndpointContract(
            urlPattern = "/api/demo/items",
            projectPath = projectPath(),
            httpMethod = "POST"
        )
        val contracts = parseArray(result)
        assertEquals(1, contracts.size())
        val contract = contracts[0]
        assertEquals("createItem", contract["methodName"].asText())

        // consumes + produces from @PostMapping
        val consumes = contract["consumes"].map { it.asText() }
        assertTrue("Expected 'application/json' in consumes=$consumes", consumes.contains("application/json"))
        val produces = contract["produces"].map { it.asText() }
        assertTrue("Expected 'application/json' in produces=$produces", produces.contains("application/json"))

        // BODY parameter from @RequestBody DemoDto
        val parameters = contract["parameters"]
        val bodyParam = parameters.firstOrNull { it["source"].asText() == "BODY" }
        assertNotNull("Expected BODY parameter in $parameters", bodyParam)
        assertTrue(
            "Expected BODY type to reference DemoDto, got ${bodyParam!!["type"].asText()}",
            bodyParam["type"].asText().contains("DemoDto")
        )

        // Response DTO schema expansion: DemoDto has id and name
        val responseSchema = contract["responseSchema"]
        assertNotNull("Expected responseSchema for DemoDto return type", responseSchema)
        assertEquals("com.example.app.dto.DemoDto", responseSchema!!["className"].asText())
        val schemaFieldNames = responseSchema["fields"].map { it["name"].asText() }.toSet()
        assertTrue("Expected id/name fields in $schemaFieldNames", schemaFieldNames.containsAll(setOf("id", "name")))

        // Service call field is always emitted; when non-null, it must point to DemoService.save.
        // Call resolution can legitimately be missing in a light fixture, so a null value is tolerated.
        val serviceCall = contract["serviceCall"]
        assertNotNull("Expected 'serviceCall' field to be present in $contract", serviceCall)
        if (serviceCall != null && !serviceCall.isNull) {
            val serviceTarget = serviceCall["target"].asText()
            assertTrue(
                "Expected serviceCall to point to DemoService.save, got $serviceTarget",
                serviceTarget.endsWith("DemoService.save")
            )
        }
    }

    fun testGetSpringDataEntitiesPackageFilterNoMatch() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getSpringDataEntities(
            projectPath = projectPath(),
            packageFilter = "com.example.does.not.exist"
        )
        val entities = parseArray(result)
        assertEquals(
            "Expected no entities for unrelated package filter, got $entities",
            0, entities.size()
        )
    }
}
