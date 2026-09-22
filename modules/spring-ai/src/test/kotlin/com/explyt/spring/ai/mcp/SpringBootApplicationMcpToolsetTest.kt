/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.runBlocking
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

class SpringBootApplicationMcpToolsetTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.jakarta_persistence_3_1_0,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    private fun projectPath(): String = project.basePath ?: ""

    private fun parseArray(json: String): JsonNode = mapper.readTree(json)

    private fun texts(node: JsonNode, field: String): List<String> =
        node.mapNotNull { it[field]?.asText() }

    private fun endpointsOf(json: String): JsonNode = mapper.readTree(json)["endpoints"]

    private fun addKotlinController() {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject("com/example/app/web/KotlinController.kt", """
            package com.example.app.web

            import com.example.app.service.DemoService
            import org.springframework.http.ResponseEntity
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController
            import org.springframework.stereotype.Service

            interface RecordLookup {
                fun findById(id: Long): com.example.app.dto.DemoDto
            }

            @Service
            class RecordLookupImpl(private val demoService: DemoService) : RecordLookup {
                override fun findById(id: Long) = demoService.findById(id)
            }

            @RestController
            @RequestMapping("/api/kotlin")
            class KotlinController(private val service: DemoService, private val records: RecordLookup) {
                @GetMapping("/wrapped/{id}")
                fun wrapped(id: Long) = ResponseEntity.ok(service.findById(id))

                @GetMapping("/interface/{id}")
                fun viaInterface(id: Long) = ResponseEntity.ok(records.findById(id))

                @GetMapping("/trimmed")
                fun trimmed(sourceKey: String): ResponseEntity<*> {
                    val key = sourceKey.trim()
                    return ResponseEntity.ok(service.findById(key.toLong()))
                }

                @GetMapping("/no-service")
                fun noService(sourceKey: String) = ResponseEntity.ok(sourceKey.trim())
            }
        """.trimIndent())
    }

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

    /**
     * A `@Bean` whose return type comes from the JDK belongs to the application just as much as a `@Service`.
     *
     * The listing used to take the declaring module from the bean's *type*, so `java.time.Clock` resolved to no
     * project module and the bean was dropped - invisible to a caller asking what the application declares.
     */
    fun testApplicationBeansKeepsALibraryTypedFactoryBean() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("beanQuery", "")

        val result = toolset.applicationBeans(
            applicationClassName = "com.explyt.demo.App",
            projectPath = projectPath(),
            beanType = "COMPONENT"
        )

        val beans = parseArray(result)
        assertTrue("The listing must stay a plain array", beans.isArray)
        val clock = beans.firstOrNull { it["beanName"].asText() == "systemClock" }
        assertNotNull("Expected systemClock among ${texts(beans, "beanName")}", clock)
        assertEquals("java.time.Clock", clock!!["className"].asText())
        assertEquals(
            "The successful schema must not gain or lose fields",
            setOf("beanName", "className", "moduleName"),
            clock.fieldNames().asSequence().toSet()
        )
    }

    fun testFindEndpoint() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.findEndpoint(
            urlPattern = "/api/demo/items/{id}",
            projectPath = projectPath(),
            httpMethod = "GET"
        )
        val endpoints = endpointsOf(result)

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
        val root = mapper.readTree(result)
        assertEquals("Expected no matching endpoints, got ${root["endpoints"]}", 0, root["endpoints"].size())
        assertEquals(0, root["totalCount"].asInt())
        assertEquals(false, root["truncated"].asBoolean())
        assertEquals("A pattern sharing no leading segment with any route has no neighbourhood",
            0, root["nearestByPrefix"].size())
        assertTrue(root["sharedPrefix"].isNull)
    }

    /**
     * A miss is the moment the tool is most useful to an agent about to add a route, and `[]` told it nothing.
     * The routes sharing the longest leading path with the pattern name the controller the new route belongs
     * to and the conventions it has to follow.
     */
    fun testFindEndpointMissListsTheNearestRoutes() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = mapper.readTree(
            toolset.findEndpoint(urlPattern = "/api/routes/export/preview", projectPath = projectPath(), httpMethod = "GET")
        )

        assertEquals(0, root["endpoints"].size())
        assertEquals("/api/routes/export", root["sharedPrefix"].asText())
        assertEquals(
            "Expected the two /api/routes siblings and nothing from the other controllers",
            listOf("/api/routes/export", "/api/routes/{id}"),
            texts(root["nearestByPrefix"], "fullPath")
        )
        val nearest = root["nearestByPrefix"].first()
        assertEquals("com.example.app.web.RouteController", nearest["controllerClass"].asText())
        assertFalse("The neighbourhood is compact: no per-method signature", nearest.has("parameters"))
    }

    /** The neighbourhood is context, not an answer, so the HTTP method filter must not thin it out. */
    fun testNearestRoutesIgnoreTheHttpMethodFilter() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = mapper.readTree(
            toolset.findEndpoint(urlPattern = "/api/demo/items/{id}/history", projectPath = projectPath(), httpMethod = "DELETE")
        )

        assertEquals(0, root["endpoints"].size())
        assertEquals(
            setOf("/api/demo/items/{id}"),
            texts(root["nearestByPrefix"], "fullPath").toSet()
        )
    }

    /**
     * When a literal route and a `{template}` route both match one URL, Spring dispatches to the literal one.
     * A caller asking "which handler serves this URL" reads the answer off the first element, so the order is
     * part of the contract, not presentation.
     */
    fun testFindEndpointOrdersTheDispatchingRouteFirst() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = mapper.readTree(
            toolset.findEndpoint(urlPattern = "/api/routes/export", projectPath = projectPath(), httpMethod = "GET")
        )

        assertEquals(
            listOf("/api/routes/export", "/api/routes/{id}"),
            texts(root["endpoints"], "fullPath")
        )
        assertEquals(2, root["totalCount"].asInt())
        assertEquals("Nothing nearest is reported when something matched", 0, root["nearestByPrefix"].size())
    }

    fun testGetHttpEndpoints() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "DemoController",
            endpointType = ""
        )
        val root = mapper.readTree(result)
        val endpoints = root["endpoints"]
        val methodNames = texts(endpoints, "methodName").toSet()
        assertTrue("Expected getItem in $methodNames", methodNames.contains("getItem"))
        assertTrue("Expected createItem in $methodNames", methodNames.contains("createItem"))

        // Wrapper metadata: totalCount matches the returned array and the small fixture is not truncated.
        assertEquals(
            "Expected totalCount to match endpoints array size",
            endpoints.size(), root["totalCount"].asInt()
        )
        assertEquals("Expected truncated=false for the small fixture", false, root["truncated"].asBoolean())
    }

    fun testGetEndpointContract() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getEndpointContract(
            urlPattern = "/api/demo/items/{id}",
            projectPath = projectPath(),
            httpMethod = "GET"
        )
        val contracts = endpointsOf(result)
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

    fun testEndpointContractFindsServiceInsideResponseEntity() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/demo/wrapped/{id}",
            projectPath = projectPath(),
            httpMethod = "GET",
        ))[0]
        val serviceCall = contract["serviceCall"]

        assertEquals("com.example.app.service.DemoService.findById", serviceCall["target"].asText())
        assertTrue(serviceCall["filePath"].asText().endsWith("com/example/app/service/DemoService.java"))
        assertEquals(33, serviceCall["line"].asInt())
    }

    fun testEndpointContractFindsServiceInKotlinExpressionBody() = runBlocking<Unit> {
        addKotlinController()

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/kotlin/wrapped/{id}",
            projectPath = projectPath(),
        ))[0]

        assertEquals("com.example.app.service.DemoService.findById", contract["serviceCall"]["target"].asText())
    }

    fun testEndpointContractSkipsTrimBeforeServiceCall() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/demo/trimmed",
            projectPath = projectPath(),
        ))[0]

        assertEquals("com.example.app.service.DemoService.findById", contract["serviceCall"]["target"].asText())
    }

    fun testEndpointContractSkipsKotlinTrimBeforeServiceCall() = runBlocking<Unit> {
        addKotlinController()
        val controller = JavaPsiFacade.getInstance(project)
            .findClass("com.example.app.web.KotlinController", GlobalSearchScope.allScope(project))!!
        val method = controller.findMethodsByName("trimmed", false).single().toUElement() as UMethod
        val trimTargets = mutableListOf<String>()
        method.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (node.methodName == "trim") {
                    node.resolve()?.let { trimTargets += "${it.containingClass?.qualifiedName}.${it.name}" }
                }
                return false
            }
        })
        assertTrue("Fixture must resolve kotlin.text.StringsKt.trim, got $trimTargets",
            "kotlin.text.StringsKt.trim" in trimTargets)

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/kotlin/trimmed",
            projectPath = projectPath(),
        )).single()

        assertEquals("com.example.app.service.DemoService.findById", contract["serviceCall"]["target"].asText())
    }

    fun testEndpointContractHasNoServiceCallForFrameworkOnlyHandler() = runBlocking<Unit> {
        addKotlinController()

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/kotlin/no-service",
            projectPath = projectPath(),
        ))[0]

        assertTrue(contract["serviceCall"].isNull)
    }

    fun testEndpointContractFindsServiceThroughInjectedInterface() = runBlocking<Unit> {
        addKotlinController()

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/kotlin/interface/{id}",
            projectPath = projectPath(),
        ))[0]

        val serviceCall = contract["serviceCall"]
        assertEquals("com.example.app.web.RecordLookup.findById", serviceCall["target"].asText())
        assertTrue(serviceCall["filePath"].asText().endsWith("com/example/app/web/KotlinController.kt"))
        assertEquals(11, serviceCall["line"].asInt())
    }

    fun testEndpointContractRejectsManuallyCreatedServiceField() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/demo/manually-created/{id}",
            projectPath = projectPath(),
        ))[0]

        assertTrue(contract["serviceCall"].isNull)
    }

    fun testEndpointContractRejectsServiceFieldBuiltInsideConstructor() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject("com/example/app/web/LocallyCreatedController.java", """
            package com.example.app.web;

            import com.example.app.dto.DemoDto;
            import com.example.app.service.DemoService;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class LocallyCreatedController {
                private final DemoService service;

                public LocallyCreatedController(DemoService service) {
                    this.service = new DemoService(null);
                }

                @GetMapping("/api/local/{id}")
                public DemoDto get(Long id) {
                    return service.findById(id);
                }
            }
        """.trimIndent())

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/local/{id}",
            projectPath = projectPath(),
        )).single()

        assertTrue(contract["serviceCall"].isNull)
    }

    fun testEndpointContractPreservesNestedKotlinGenericNullability() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject("com/example/app/web/SchemaController.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            data class RecordsDto(
                val rows: List<List<String?>>,
                val solid: List<List<String>>,
                val nullableRow: List<List<String>?>,
            )

            @RestController
            class SchemaController {
                @GetMapping("/api/schema/rows")
                fun rows(): RecordsDto = RecordsDto(emptyList(), emptyList(), emptyList())
            }
        """.trimIndent())

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/schema/rows",
            projectPath = projectPath(),
        )).single()
        val fields = contract["responseSchema"]["fields"].associateBy { it["name"].asText() }

        assertEquals("java.util.List<java.util.List<java.lang.String?>>", fields.getValue("rows")["type"].asText())
        assertEquals("java.util.List<java.util.List<java.lang.String>>", fields.getValue("solid")["type"].asText())
        assertEquals("java.util.List<java.util.List<java.lang.String>?>", fields.getValue("nullableRow")["type"].asText())
        assertFalse(fields.getValue("rows")["nullable"].asBoolean())
    }

    private fun addMultipartController() {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject("com/example/app/web/MultipartController.java", """
            package com.example.app.web;

            import com.example.app.dto.DemoDto;
            import java.util.List;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RequestPart;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.multipart.MultipartFile;

            @RestController
            public class MultipartController {
                @PostMapping("/api/multipart/upload")
                public String upload(
                        @RequestParam("file") MultipartFile upload,
                        @RequestParam("files") List<MultipartFile> files,
                        @RequestParam("images") MultipartFile[] images,
                        @RequestPart("metadata") DemoDto dto,
                        @RequestPart(value = "optional", required = false) DemoDto opt) {
                    return "ok";
                }

                @GetMapping("/api/multipart/find")
                public String find(@RequestParam("q") String query) {
                    return query;
                }

                @PostMapping("/api/multipart/servlet")
                public String servlet(@RequestParam("servletPart") jakarta.servlet.http.Part part) {
                    return part.getName();
                }

                @PostMapping("/api/multipart/named-map")
                public String namedMap(@RequestParam("values") java.util.Map<String, MultipartFile> values) {
                    return "ok";
                }
            }
        """.trimIndent())
    }

    fun testEndpointContractReportsKotlinMultipartFile() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")
        myFixture.addFileToProject("com/example/app/web/KotlinMultipartController.kt", """
            package com.example.app.web

            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController
            import org.springframework.web.multipart.MultipartFile

            @RestController
            class KotlinMultipartController {
                @PostMapping("/api/multipart/kotlin")
                fun upload(@RequestParam("file") upload: MultipartFile): String = upload.name
            }
        """.trimIndent())

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/multipart/kotlin",
            projectPath = projectPath(),
        )).single()
        val parameters = contract["parameters"]

        assertEquals(1, parameters.size())
        assertEquals("file", parameters[0]["name"].asText())
        assertEquals("PART", parameters[0]["source"].asText())
        assertEquals("org.springframework.web.multipart.MultipartFile", parameters[0]["type"].asText())
    }

    fun testEndpointContractReportsMultipartParts() = runBlocking<Unit> {
        addMultipartController()

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/multipart/upload",
            projectPath = projectPath(),
        )).single()
        val parameters = contract["parameters"]
        val byName = parameters.associateBy { it["name"].asText() }

        assertEquals(5, parameters.size())
        assertEquals("PART", byName.getValue("file")["source"].asText())
        assertEquals(setOf("file", "files", "images", "metadata", "optional"), byName.keys)
        assertEquals("PART", byName.getValue("files")["source"].asText())
        assertEquals("PART", byName.getValue("images")["source"].asText())
        assertEquals("PART", byName.getValue("metadata")["source"].asText())
        assertEquals("PART", byName.getValue("optional")["source"].asText())
        assertTrue(byName.getValue("metadata")["required"].asBoolean())
        assertFalse(byName.getValue("optional")["required"].asBoolean())
        assertEquals("org.springframework.web.multipart.MultipartFile", byName.getValue("file")["type"].asText())
        assertEquals("com.example.app.dto.DemoDto", byName.getValue("metadata")["type"].asText())
    }

    fun testNamedMapOfMultipartFilesIsNotAFilePart() = runBlocking<Unit> {
        addMultipartController()

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/multipart/named-map",
            projectPath = projectPath(),
        )).single()
        val parameter = contract["parameters"].single()

        assertEquals("values", parameter["name"].asText())
        assertEquals("QUERY", parameter["source"].asText())
    }

    fun testEndpointContractReportsServletMultipartPart() = runBlocking<Unit> {
        addMultipartController()

        val contract = endpointsOf(toolset.getEndpointContract(
            urlPattern = "/api/multipart/servlet",
            projectPath = projectPath(),
        )).single()
        val parameter = contract["parameters"].single()

        assertEquals("servletPart", parameter["name"].asText())
        assertEquals("PART", parameter["source"].asText())
        assertEquals("jakarta.servlet.http.Part", parameter["type"].asText())
        assertTrue(parameter["required"].asBoolean())
    }

    fun testHttpEndpointListingKeepsMultipartAndQuerySources() = runBlocking<Unit> {
        addMultipartController()

        val endpoints = endpointsOf(toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "MultipartController",
        ))
        val upload = endpoints.single { it["methodName"].asText() == "upload" }
        val find = endpoints.single { it["methodName"].asText() == "find" }
        val uploadParams = upload["parameters"]

        assertEquals("Expected upload parameters in $upload", 5, uploadParams.size())
        assertEquals(5, uploadParams.count { it["source"].asText() == "PART" })
        assertEquals("QUERY", find["parameters"].single()["source"].asText())
        assertEquals("q", find["parameters"].single()["name"].asText())
    }

    /**
     * Every handler parameter must appear in the contract, whatever binds it.
     *
     * A parameter bound by a custom `HandlerMethodArgumentResolver` carries no binding annotation, so an
     * enumeration driven purely by annotations dropped it silently — and a dropped parameter is
     * indistinguishable from one that was never declared. The two have opposite meanings when the parameter is
     * the one carrying authorization: reading the tool's output alone, an endpoint that authenticates its caller
     * looks exactly like one that does not.
     */
    fun testEndpointContractReportsEveryParameterSource() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getEndpointContract(
            urlPattern = "/api/resolver/items/{id}",
            projectPath = projectPath(),
            httpMethod = "GET"
        )
        val contract = endpointsOf(result)[0]
        val parameters = contract["parameters"]
        val bySource = parameters.associate { it["name"].asText() to it["source"].asText() }

        assertEquals(
            "Every declared parameter must be reported, whatever binds it",
            mapOf(
                "id" to "PATH",
                "q" to "QUERY",
                "sid" to "COOKIE",
                "currentUser" to "UNKNOWN",
                "webRequest" to "FRAMEWORK",
                "locale" to "FRAMEWORK",
            ),
            bySource
        )
    }

    /** The same completeness must hold for the endpoint listing, which shares the parameter enumeration. */
    fun testHttpEndpointsReportTheResolverBoundParameter() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "ResolverController",
            endpointType = ""
        )
        val endpoint = endpointsOf(result).first { it["methodName"].asText() == "getItem" }
        val names = endpoint["parameters"].map { it["name"].asText() }

        assertTrue(
            "Expected the resolver-bound 'currentUser' to be listed, got $names",
            names.contains("currentUser")
        )
    }

    /**
     * An unclassified parameter must not claim a requiredness it cannot know. The four annotation-bound sources
     * declare it; a resolver's contract is private to the resolver, so `required` stays null rather than
     * defaulting to a plausible-looking boolean.
     */
    fun testUnclassifiedParameterDoesNotAssertRequiredness() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getEndpointContract(
            urlPattern = "/api/resolver/items/{id}",
            projectPath = projectPath(),
            httpMethod = "GET"
        )
        val parameters = endpointsOf(result)[0]["parameters"]

        val currentUser = parameters.first { it["name"].asText() == "currentUser" }
        assertTrue("Expected 'required' to be null for a resolver-bound parameter", currentUser["required"].isNull)

        val pathParam = parameters.first { it["name"].asText() == "id" }
        assertEquals("A @PathVariable still declares its requiredness", true, pathParam["required"].asBoolean())
    }

    /**
     * The listing is the tool called *first*, before the caller knows which controllers exist — exactly when the
     * `controllerFilter` / `endpointType` filters cannot help. On a real project it returned 136 endpoints as
     * 122 722 characters of single-line JSON, most of it `parameters` arrays, which a line-based reader cannot
     * chunk. `compact` drops the two per-endpoint arrays so an inventory fits in a context window.
     */
    fun testCompactListingOmitsParametersAndReturnType() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val endpoint = endpointsOf(
            toolset.getHttpEndpoints(
                projectPath = projectPath(),
                controllerFilter = "ResolverController",
                endpointType = "",
                compact = true
            )
        ).first { it["methodName"].asText() == "getItem" }

        assertFalse("compact must omit 'parameters', got $endpoint", endpoint.has("parameters"))
        assertFalse("compact must omit 'returnType', got $endpoint", endpoint.has("returnType"))
        // Everything an inventory needs is still there.
        assertEquals("/api/resolver/items/{id}", endpoint["fullPath"].asText())
        assertEquals("com.example.app.web.ResolverController", endpoint["controllerClass"].asText())
        assertTrue("Expected a line number in $endpoint", endpoint["line"].isInt)
    }

    /** The default stays whole: `compact` is opt-in, so an existing caller sees no change. */
    fun testDefaultListingKeepsParametersAndReturnType() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val endpoint = endpointsOf(
            toolset.getHttpEndpoints(
                projectPath = projectPath(),
                controllerFilter = "ResolverController",
                endpointType = ""
            )
        ).first { it["methodName"].asText() == "getItem" }

        assertTrue("The default listing must keep 'parameters', got $endpoint", endpoint.has("parameters"))
        assertTrue("The default listing must keep 'returnType', got $endpoint", endpoint.has("returnType"))
    }

    /** Paging metadata describes the page, not the projection, so it must survive `compact`. */
    fun testCompactListingKeepsPagingMetadata() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val root = mapper.readTree(
            toolset.getHttpEndpoints(
                projectPath = projectPath(),
                controllerFilter = "",
                endpointType = "",
                compact = true
            )
        )

        assertEquals("totalCount must match the returned page for a small fixture",
            root["endpoints"].size(), root["totalCount"].asInt())
        assertEquals(0, root["offset"].asInt())
        assertEquals(false, root["truncated"].asBoolean())
    }

    /**
     * The response field names are part of the tool's contract, and a mismatch between them and the description
     * is not cosmetic: a first parse written against `httpMethod` / `path` / `lineNumber` yields silent nulls
     * rather than an error, costing a whole call to notice. This pins the names the description promises.
     */
    fun testListingFieldNamesMatchTheDocumentedContract() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val endpoint = endpointsOf(
            toolset.getHttpEndpoints(projectPath = projectPath(), controllerFilter = "DemoController", endpointType = "")
        ).first()

        val names = endpoint.fieldNames().asSequence().toSet()
        assertEquals(
            "Endpoint field names drifted from the documented contract",
            setOf(
                "httpMethods", "fullPath", "controllerClass", "methodName",
                "filePath", "line", "parameters", "returnType", "endpointType",
            ),
            names
        )
        assertTrue("'httpMethods' is an array, not a scalar", endpoint["httpMethods"].isArray)
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

    fun testLineOfElementWithoutTextRange() {
        val physicalElement = PsiTreeUtil.findChildOfType(
            myFixture.configureByText("Controller.java", "\n\nclass Controller {}"),
            PsiClass::class.java
        )!!
        val elementWithoutTextRange = object : FakePsiElement() {
            override fun getParent(): PsiElement = physicalElement
            override fun getName(): String = "Controller"
            override fun getNavigationElement(): PsiElement = physicalElement
        }

        assertNull(elementWithoutTextRange.textRange)
        assertEquals(3, toolset.lineOf(physicalElement))
        assertEquals(3, toolset.lineOf(elementWithoutTextRange))
    }

    fun testLineOfLightMethodUsesNavigationTarget() {
        // A light method without a text range still resolves through the physical method it navigates to.
        val declaringClass = physicalClass()
        val physicalMethod = declaringClass.methods.single()
        val lightMethod = object : LightMethod(psiManager, physicalMethod, declaringClass) {
            override fun getTextRange(): TextRange? = null
            override fun getNavigationElement(): PsiElement = physicalMethod
        }

        assertNull("Expected a light method without a text range", lightMethod.textRange)
        assertEquals(3, toolset.lineOf(lightMethod))
    }

    fun testLineOfSyntheticMethodFallsBackToContainingClass() {
        // Reproduces issue #281: a synthetic member - a generated `copy()`, an enum `values()`, or a Kotlin
        // light method whose origin declaration is absent - has no text range and navigates to itself, so it
        // has no source position at all. Its declaring class is the closest physical anchor.
        val declaringClass = physicalClass()
        val syntheticMethod = object : LightMethod(psiManager, declaringClass.methods.single(), declaringClass) {
            override fun getTextRange(): TextRange? = null
            override fun getNavigationElement(): PsiElement = this
        }

        assertNull("Expected a synthetic method without a text range", syntheticMethod.textRange)
        assertSame(
            "Expected a synthetic method navigating to itself",
            syntheticMethod, syntheticMethod.navigationElement
        )
        assertEquals(3, toolset.lineOf(syntheticMethod))
    }

    private fun physicalClass(): PsiClass = PsiTreeUtil.findChildOfType(
        myFixture.configureByText("Controller.java", "\n\nclass Controller { void handle() {} }"),
        PsiClass::class.java
    )!!

    fun testLineOfElementWithoutSourceRangeIsUnknown() {
        // The fixture class sits on line 3, so a genuine lookup could never return `null`.
        val physicalElement = myFixture.configureByText("Controller.java", "\n\nclass Controller {}")
        val elementWithoutSourceRange = object : FakePsiElement() {
            override fun getParent(): PsiElement = physicalElement
            override fun getName(): String = "Controller"
        }

        assertNull(elementWithoutSourceRange.textRange)
        assertNull(
            "Expected an unknown line rather than a fabricated one",
            toolset.lineOf(elementWithoutSourceRange)
        )
    }

    fun testGetSpringDataEntities() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val result = toolset.getSpringDataEntities(
            projectPath = projectPath(),
            packageFilter = "com.example.app",
            includeDetails = true
        )
        val page = parseArray(result)
        assertEquals("The fixture declares four entities under 'com.example.app'", 4, page["totalCount"].asInt())
        val entities = page["entities"]
        val entity = entities.first { it["className"].asText() == "com.example.app.entity.DemoEntity" }
        assertEquals("DemoEntity", entity["name"].asText())
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
        val methodNames = texts(endpointsOf(result), "methodName").toSet()
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
        val endpoints = endpointsOf(result)
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
        val mvcMethodNames = texts(endpointsOf(mvcResult), "methodName").toSet()
        assertTrue(
            "Expected Spring MVC endpoints (getItem, createItem), got $mvcMethodNames",
            mvcMethodNames.containsAll(setOf("getItem", "createItem"))
        )

        val feignResult = toolset.getHttpEndpoints(
            projectPath = projectPath(),
            controllerFilter = "",
            endpointType = "SPRING_OPEN_FEIGN"
        )
        val feignEndpoints = endpointsOf(feignResult)
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
        val endpoints = endpointsOf(result)
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
        val contracts = endpointsOf(result)
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
        val page = parseArray(result)
        assertEquals(
            "Expected no entities for unrelated package filter, got $page",
            0, page["totalCount"].asInt()
        )
        assertEquals(0, page["entities"].size())
    }
}
