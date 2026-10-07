/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * The contract of an endpoint whose mapping and binding annotations live on an implemented interface - the shape
 * openapi-generator produces - while the controller overrides the methods without repeating them.
 *
 * Spring invokes the override (`MethodIntrospector.selectMethods` keys by `ClassUtils.getMostSpecificMethod`), binds its
 * parameters with the annotations of the overridden interface parameters (`AnnotatedMethod.getInheritedParameterAnnotations`)
 * and takes the media types from the merged `@RequestMapping`.
 */
class SpringBootApplicationMcpToolsetInterfaceMappingTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    override val realJdk: Boolean = true

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/example/app/AppDto.java", "package com.example.app;\n\npublic class AppDto { public String name; }\n"
        )
        myFixture.addFileToProject(
            "com/example/app/AppService.java", """
            package com.example.app;

            import org.springframework.stereotype.Service;

            @Service
            public class AppService {
                public AppDto find(String appId, String view, String tenant) { return new AppDto(); }
                public AppDto create(AppDto body) { return body; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/AppApi.java", """
            package com.example.app;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;

            public interface AppApi {
                @GetMapping("/apps/{appId}")
                default ResponseEntity<Object> getApp(@PathVariable("appId") String appId,
                                                      @RequestParam(required = false) String view,
                                                      @RequestHeader("X-Tenant") String tenant) {
                    return ResponseEntity.ok().build();
                }

                @PostMapping(value = "/apps", consumes = "application/json", produces = "application/json")
                ResponseEntity<?> create(@RequestBody AppDto body);
            }
            """.trimIndent()
        )
    }

    fun testParametersDeclaredOnTheInterfaceKeepTheirSources() = runBlocking<Unit> {
        addUnannotatedController()

        val contract = contractOf("/apps/{appId}", "GET")

        assertEquals(
            mapOf("appId" to "PATH", "view" to "QUERY", "X-Tenant" to "HEADER"),
            sourcesOf(contract["parameters"]),
        )
        assertFalse("view is optional on the interface", parameter(contract, "view")["required"].asBoolean(true))
    }

    fun testRequestBodyDeclaredOnTheInterfaceIsTheBody() = runBlocking<Unit> {
        addUnannotatedController()

        val contract = contractOf("/apps", "POST")

        assertEquals(mapOf("body" to "BODY"), sourcesOf(contract["parameters"]))
        assertEquals("com.example.app.AppDto", parameter(contract, "body")["type"].asText())
    }

    fun testMediaTypesDeclaredOnTheInterfaceMappingAreReported() = runBlocking<Unit> {
        addUnannotatedController()

        val contract = contractOf("/apps", "POST")

        assertEquals(listOf("application/json"), contract["consumes"].map { it.asText() })
        assertEquals(listOf("application/json"), contract["produces"].map { it.asText() })
    }

    fun testReturnTypeAndServiceCallsAreTheOverrides() = runBlocking<Unit> {
        addUnannotatedController()

        val contract = contractOf("/apps/{appId}", "GET")

        assertEquals("org.springframework.http.ResponseEntity<com.example.app.AppDto>", contract["returnType"].asText())
        assertEquals(listOf("com.example.app.AppService.find"), contract["serviceCalls"].map { it["target"].asText() })
    }

    /** `AnnotatedMethodParameter` keeps the handler's own annotation; an inherited one of the same type is skipped. */
    fun testAnnotationsOnTheOverrideWinOverTheInterface() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/app/AppController.java", """
            package com.example.app;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class AppController implements AppApi {
                @Override
                public ResponseEntity<Object> getApp(@PathVariable("appId") String appId,
                                                     @RequestParam(name = "mode") String view,
                                                     @RequestHeader("X-Org") String tenant) {
                    return ResponseEntity.ok().build();
                }

                @Override
                public ResponseEntity<AppDto> create(AppDto body) { return ResponseEntity.ok(body); }
            }
            """.trimIndent()
        )

        val contract = contractOf("/apps/{appId}", "GET")

        assertEquals(
            mapOf("appId" to "PATH", "mode" to "QUERY", "X-Org" to "HEADER"),
            sourcesOf(contract["parameters"]),
        )
    }

    fun testOrdinaryAnnotatedControllerIsUnchanged() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/app/PlainController.java", """
            package com.example.app;

            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class PlainController {
                @PostMapping(value = "/plain/{id}", consumes = "application/json", produces = "application/json")
                public AppDto create(@PathVariable String id, @RequestParam(required = false) String view,
                                     @RequestHeader("X-Tenant") String tenant, @RequestBody AppDto body) {
                    return body;
                }
            }
            """.trimIndent()
        )

        val contract = contractOf("/plain/{id}", "POST")

        assertEquals(
            mapOf("id" to "PATH", "view" to "QUERY", "X-Tenant" to "HEADER", "body" to "BODY"),
            sourcesOf(contract["parameters"]),
        )
        assertEquals(listOf("application/json"), contract["consumes"].map { it.asText() })
        assertEquals(listOf("application/json"), contract["produces"].map { it.asText() })
    }

    private fun addUnannotatedController() {
        myFixture.addFileToProject(
            "com/example/app/AppController.java", """
            package com.example.app;

            import org.springframework.http.ResponseEntity;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class AppController implements AppApi {
                private final AppService service;

                public AppController(AppService service) { this.service = service; }

                @Override
                public ResponseEntity<AppDto> getApp(String appId, String view, String tenant) {
                    return ResponseEntity.ok(service.find(appId, view, tenant));
                }

                @Override
                public ResponseEntity<AppDto> create(AppDto body) {
                    return ResponseEntity.ok(service.create(body));
                }
            }
            """.trimIndent()
        )
    }

    /** The contract of [fullPath], asserted to be served by the controller's override. */
    private suspend fun contractOf(fullPath: String, verb: String): JsonNode {
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = fullPath, projectPath = project.basePath, httpMethod = verb)
        )["endpoints"].filter { it["fullPath"].asText() == fullPath }
        assertEquals("Precondition: one $verb $fullPath contract, got $endpoints", 1, endpoints.size)
        val contract = endpoints.single()
        assertTrue(
            "Precondition: the contract is read from the controller, got ${contract["controllerClass"]}",
            contract["controllerClass"].asText().endsWith("Controller")
        )
        return contract
    }

    private fun sourcesOf(parameters: JsonNode): Map<String, String> =
        parameters.associate { it["name"].asText() to it["source"].asText() }

    private fun parameter(contract: JsonNode, name: String): JsonNode =
        contract["parameters"].singleOrNull { it["name"].asText() == name }
            ?: error("no parameter $name in ${contract["parameters"]}")
}
