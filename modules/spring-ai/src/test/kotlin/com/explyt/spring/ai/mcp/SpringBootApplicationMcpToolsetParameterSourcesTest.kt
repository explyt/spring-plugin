/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.beans.SpringBeanMcpToolset
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.util.WebApplicationStack
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.psi.JavaPsiFacade
import kotlinx.coroutines.runBlocking

/**
 * Where the endpoint tools say an unannotated handler parameter comes from.
 *
 * Spring's resolver chain ends in two catch-alls: `RequestParamMethodArgumentResolver(useDefaultResolution = true)`
 * takes a type `BeanUtils.isSimpleProperty` calls simple, and the model-attribute resolver
 * (`annotationNotRequired = true`) takes every other one. Reporting such a parameter as `UNKNOWN` read as "a custom
 * resolver binds it", and hid the `?lastName=` search a form object carries.
 */
class SpringBootApplicationMcpToolsetParameterSourcesTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.kotlin_1_9_22,
        TestLibrary("jakarta.servlet:jakarta.servlet-api:6.0.0"),
    )

    override val realJdk: Boolean = true

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testUnannotatedFormObjectIsAModelAttributeBesideAPrimitivePathVariable() = runBlocking<Unit> {
        addJavaOwnerController()

        val parameters = contractParameters("/owners/7/edit", "/owners/{ownerId}/edit")

        assertEquals(
            mapOf("owner" to "MODEL", "result" to "FRAMEWORK", "ownerId" to "PATH"),
            sourcesOf(parameters),
        )
    }

    fun testFindFormBindsTheOwnerAsAModelAttributeNextToADefaultedPage() = runBlocking<Unit> {
        addJavaOwnerController()

        val parameters = contractParameters("/owners")

        assertEquals(
            mapOf("page" to "QUERY", "owner" to "MODEL", "result" to "FRAMEWORK", "model" to "FRAMEWORK"),
            sourcesOf(parameters),
        )
    }

    /** `BeanUtils.isSimpleProperty`: `CharSequence`, `Number`, `Temporal`, `Enum`, `UUID` and their arrays. */
    fun testUnannotatedSimpleTypesAreQueryParametersNamedAfterTheParameter() = runBlocking<Unit> {
        addJavaSearchController()

        val sources = sourcesOf(contractParameters("/search"))

        assertEquals(
            mapOf(
                "lastName" to "QUERY",
                "since" to "QUERY",
                "status" to "QUERY",
                "token" to "QUERY",
                "limit" to "QUERY",
                "tags" to "QUERY",
                "filter" to "MODEL",
                "names" to "MODEL",
            ),
            sources,
        )
    }

    fun testKotlinUnannotatedParametersFollowTheSameRule() = runBlocking<Unit> {
        addKotlinPetController()

        val parameters = contractParameters("/pets/3", "/pets/{petId}")

        assertEquals(
            mapOf("petId" to "PATH", "name" to "QUERY", "page" to "QUERY", "pet" to "MODEL"),
            sourcesOf(parameters),
        )
        assertEquals("int", typeOf(parameters, "page"))
    }

    fun testPrimitiveParameterReportsTheDeclaredType() = runBlocking<Unit> {
        addJavaOwnerController()

        val edit = contractParameters("/owners/7/edit", "/owners/{ownerId}/edit")
        val find = contractParameters("/owners")

        assertEquals("int", typeOf(edit, "ownerId"))
        assertEquals("int", typeOf(find, "page"))
    }

    fun testFindEndpointReportsTheSameSourcesAsTheContract() = runBlocking<Unit> {
        addJavaOwnerController()

        val found = mapper.readTree(toolset.findEndpoint(urlPattern = "/owners/7/edit", projectPath = projectPath()))
        val parameters = found["endpoints"].single()["parameters"]

        assertEquals(
            mapOf("owner" to "MODEL", "result" to "FRAMEWORK", "ownerId" to "PATH"),
            sourcesOf(parameters),
        )
        assertEquals("int", typeOf(parameters, "ownerId"))
    }

    /** An annotation Spring does not know may be the key of a project resolver: the tool cannot say what binds it. */
    fun testAParameterUnderAnUnknownAnnotationStaysUnknown() = runBlocking<Unit> {
        addJavaSearchController()

        val sources = sourcesOf(contractParameters("/me"))

        assertEquals(mapOf("caller" to "UNKNOWN"), sources)
    }

    fun testNestedApplicationClassResolvesWithADollarInTheBeanListing() = runBlocking<Unit> {
        addNestedApplication()

        val beans = mapper.readTree(
            toolset.applicationBeans(
                applicationClassName = "com.example.app.PetClinicTests\$TestConfiguration",
                projectPath = projectPath(),
                beanType = "COMPONENT",
                source = "STATIC",
            )
        )

        assertTrue("Expected the nested application's component, got $beans", beans.any {
            it["className"].asText() == "com.example.app.PetClinicTests.Clock"
        })
    }

    fun testNestedApplicationClassResolvesWithADollarInTheBeanLookup() = runBlocking<Unit> {
        addNestedApplication()

        val root = mapper.readTree(
            beanToolset().findSpringBean(
                projectPath = projectPath(),
                applicationClassName = "com.example.app.PetClinicTests\$TestConfiguration",
                source = "STATIC",
                beanName = "clock",
            )
        )

        assertEquals("OK", root["status"].asText())
        assertEquals("com.example.app.PetClinicTests.TestConfiguration", root["model"]["application"].asText())
    }

    fun testMultipartRequestInterfacesAreSuppliedByTheServletRequestResolver() = runBlocking<Unit> {
        assertServletStackResolving(MULTIPART_REQUEST, MULTIPART_HTTP_SERVLET_REQUEST, HTTP_SERVLET_REQUEST)
        addJavaServletArgumentsController()

        assertEquals(
            mapOf("multipart" to "FRAMEWORK", "multipartServlet" to "FRAMEWORK", "request" to "FRAMEWORK"),
            sourcesOf(contractParameters("/servlet/multipart")),
        )
    }

    fun testPushBuilderIsSuppliedByTheServletRequestResolver() = runBlocking<Unit> {
        assertServletStackResolving(PUSH_BUILDER, HTTP_SERVLET_REQUEST)
        addJavaServletArgumentsController()

        assertEquals(
            mapOf("push" to "FRAMEWORK", "request" to "FRAMEWORK"),
            sourcesOf(contractParameters("/servlet/push")),
        )
    }

    fun testUnannotatedJakartaPartIsAMultipartPartLikeAMultipartFile() = runBlocking<Unit> {
        assertServletStackResolving(JAKARTA_PART, MULTIPART_FILE)
        addJavaServletArgumentsController()

        assertEquals(
            mapOf("attachment" to "PART", "attachments" to "PART", "file" to "PART"),
            sourcesOf(contractParameters("/servlet/part")),
        )
    }

    fun testKotlinUnannotatedJakartaPartIsAMultipartPart() = runBlocking<Unit> {
        assertServletStackResolving(JAKARTA_PART, MULTIPART_FILE)
        myFixture.addFileToProject(
            "com/example/app/AttachmentController.kt", """
            package com.example.app

            import jakarta.servlet.http.Part
            import org.springframework.web.bind.annotation.PostMapping
            import org.springframework.web.bind.annotation.RestController
            import org.springframework.web.multipart.MultipartFile

            @RestController
            class AttachmentController {
                @PostMapping("/kotlin/part")
                fun upload(attachment: Part, file: MultipartFile): String = ""
            }
            """.trimIndent()
        )

        assertEquals(
            mapOf("attachment" to "PART", "file" to "PART"),
            sourcesOf(contractParameters("/kotlin/part")),
        )
    }

    fun testZoneSubclassesMissTheExactServletPredicateAndBindAsSimpleQueryValues() = runBlocking<Unit> {
        assertServletStackResolving("java.util.SimpleTimeZone", "java.time.ZoneOffset")
        addJavaServletArgumentsController()

        assertEquals(
            mapOf("zone" to "FRAMEWORK", "simpleZone" to "QUERY", "zoneId" to "FRAMEWORK", "offset" to "QUERY"),
            sourcesOf(contractParameters("/servlet/zones")),
        )
    }

    fun testProjectSubtypesOfExactOnlyBuilderAndSessionStatusAreModelAttributes() = runBlocking<Unit> {
        assertServletStackResolving(URI_COMPONENTS_BUILDER, SERVLET_URI_COMPONENTS_BUILDER, SESSION_STATUS)
        addJavaServletArgumentsController()

        assertEquals(
            mapOf(
                "builder" to "FRAMEWORK",
                "servletBuilder" to "FRAMEWORK",
                "projectBuilder" to "MODEL",
                "status" to "FRAMEWORK",
                "projectStatus" to "MODEL",
            ),
            sourcesOf(contractParameters("/servlet/builders")),
        )
    }

    fun testServletModelMapAndMapAreTheModelWhileAHashMapIsAModelAttribute() = runBlocking<Unit> {
        assertServletStackResolving("org.springframework.ui.ModelMap", "java.util.HashMap")
        addJavaServletArgumentsController()

        assertEquals(
            mapOf("model" to "FRAMEWORK", "modelMap" to "FRAMEWORK", "map" to "FRAMEWORK", "hashMap" to "MODEL"),
            sourcesOf(contractParameters("/servlet/model")),
        )
    }

    fun testAnnotatedMapsAreNotFrameworkSuppliedOnServlet() = runBlocking<Unit> {
        assertServletStackResolving("java.util.Map", "org.springframework.web.bind.annotation.MatrixVariable", "org.springframework.web.bind.annotation.RequestAttribute")
        addAnnotatedMapController()

        assertEquals(
            mapOf(
                "matrix" to "UNKNOWN",
                "attrs" to "UNKNOWN",
                "user" to "UNKNOWN",
                "validated" to "MODEL",
                "requestParam" to "QUERY",
                "path" to "PATH",
            ),
            sourcesOf(contractParameters("/servlet/annotated/{path}")),
        )
    }

    private fun addAnnotatedMapController() {
        myFixture.addFileToProject(
            "com/example/app/CurrentUser.java", """
            package com.example.app;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface CurrentUser {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/AnnotatedMapController.java", """
            package com.example.app;

            import java.util.Map;
            import jakarta.validation.Valid;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.MatrixVariable;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestAttribute;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            class AnnotatedMapController {
                @GetMapping("/servlet/annotated/{path}")
                public String annotated(
                        @MatrixVariable Map<String, String> matrix,
                        @RequestAttribute Map<String, Object> attrs,
                        @CurrentUser Map<String, Object> user,
                        @Valid Map<String, Object> validated,
                        @RequestParam Map<String, String> requestParam,
                        @PathVariable String path
                ) {
                    return path;
                }
            }
            """.trimIndent()
        )
    }

    private fun assertServletStackResolving(vararg classNames: String) {
        assertEquals(WebApplicationStack.SERVLET, WebApplicationStack.of(module))
        val facade = JavaPsiFacade.getInstance(project)
        for (className in classNames) {
            assertNotNull("$className on the module classpath", facade.findClass(className, module.moduleWithLibrariesScope))
        }
    }

    private fun addJavaServletArgumentsController() {
        myFixture.addFileToProject(
            "com/example/app/ProjectUriComponentsBuilder.java", """
            package com.example.app;

            import org.springframework.web.util.UriComponentsBuilder;

            public class ProjectUriComponentsBuilder extends UriComponentsBuilder {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/ProjectSessionStatus.java", """
            package com.example.app;

            import org.springframework.web.bind.support.SessionStatus;

            public class ProjectSessionStatus implements SessionStatus {
                private boolean complete;
                public void setComplete() { complete = true; }
                public boolean isComplete() { return complete; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/ServletArgumentsController.java", """
            package com.example.app;

            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.Part;
            import jakarta.servlet.http.PushBuilder;
            import java.time.ZoneId;
            import java.time.ZoneOffset;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.SimpleTimeZone;
            import java.util.TimeZone;
            import org.springframework.ui.Model;
            import org.springframework.ui.ModelMap;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.bind.support.SessionStatus;
            import org.springframework.web.multipart.MultipartFile;
            import org.springframework.web.multipart.MultipartHttpServletRequest;
            import org.springframework.web.multipart.MultipartRequest;
            import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
            import org.springframework.web.util.UriComponentsBuilder;

            @RestController
            class ServletArgumentsController {
                @PostMapping("/servlet/multipart")
                public String multipart(MultipartRequest multipart, MultipartHttpServletRequest multipartServlet,
                                        HttpServletRequest request) {
                    return "";
                }

                @GetMapping("/servlet/push")
                public String push(PushBuilder push, HttpServletRequest request) {
                    return "";
                }

                @PostMapping("/servlet/part")
                public String part(Part attachment, List<Part> attachments, MultipartFile file) {
                    return "";
                }

                @GetMapping("/servlet/zones")
                public String zones(TimeZone zone, SimpleTimeZone simpleZone, ZoneId zoneId, ZoneOffset offset) {
                    return "";
                }

                @GetMapping("/servlet/builders")
                public String builders(UriComponentsBuilder builder, ServletUriComponentsBuilder servletBuilder,
                                       ProjectUriComponentsBuilder projectBuilder, SessionStatus status,
                                       ProjectSessionStatus projectStatus) {
                    return "";
                }

                @GetMapping("/servlet/model")
                public String model(Model model, ModelMap modelMap, Map<String, Object> map,
                                    HashMap<String, Object> hashMap) {
                    return "";
                }
            }
            """.trimIndent()
        )
    }

    /** Picked by its path: `/owners` also matches `/owners/{ownerId}/edit` as a containing path. */
    private suspend fun contractParameters(url: String, fullPath: String = url): JsonNode =
        mapper.readTree(toolset.getEndpointContract(urlPattern = url, projectPath = projectPath()))["endpoints"]
            .single { it["fullPath"].asText() == fullPath }["parameters"]

    private fun sourcesOf(parameters: JsonNode): Map<String, String> =
        parameters.associate { it["name"].asText() to it["source"].asText() }

    private fun typeOf(parameters: JsonNode, name: String): String =
        parameters.single { it["name"].asText() == name }["type"].asText()

    private fun beanToolset(): SpringBeanMcpToolset =
        McpToolset.EP.extensionList.filterIsInstance<SpringBeanMcpToolset>().single()

    private fun projectPath(): String = project.basePath!!

    private fun addJavaOwnerController() {
        myFixture.addFileToProject(
            "com/example/app/Owner.java", """
            package com.example.app;

            public class Owner {
                private String lastName;
                public String getLastName() { return lastName; }
                public void setLastName(String lastName) { this.lastName = lastName; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/OwnerController.java", """
            package com.example.app;

            import org.springframework.stereotype.Controller;
            import org.springframework.ui.Model;
            import org.springframework.validation.BindingResult;
            import org.springframework.validation.annotation.Validated;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestParam;

            @Controller
            class OwnerController {
                @PostMapping("/owners/{ownerId}/edit")
                public String processUpdateOwnerForm(@Validated Owner owner, BindingResult result, @PathVariable int ownerId) {
                    return "redirect:/owners/" + ownerId;
                }

                @GetMapping("/owners")
                public String processFindForm(@RequestParam(defaultValue = "1") int page, Owner owner, BindingResult result, Model model) {
                    return "owners/ownersList";
                }
            }
            """.trimIndent()
        )
    }

    private fun addJavaSearchController() {
        myFixture.addFileToProject(
            "com/example/app/Status.java", """
            package com.example.app;

            public enum Status { OPEN, CLOSED }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/Caller.java", """
            package com.example.app;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Caller {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/SearchController.java", """
            package com.example.app;

            import java.time.LocalDate;
            import java.util.List;
            import java.util.UUID;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            class SearchController {
                @GetMapping("/search")
                public String search(String lastName, LocalDate since, Status status, UUID token, Long limit,
                                     String[] tags, Owner filter, List<String> names) {
                    return lastName;
                }

                @GetMapping("/me")
                public String me(@Caller Owner caller) {
                    return "me";
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/app/Owner.java", """
            package com.example.app;

            public class Owner {
                private String lastName;
                public String getLastName() { return lastName; }
            }
            """.trimIndent()
        )
    }

    private fun addKotlinPetController() {
        myFixture.addFileToProject(
            "com/example/app/PetController.kt", """
            package com.example.app

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RestController

            class Pet(var name: String = "")

            @RestController
            class PetController {
                @GetMapping("/pets/{petId}")
                fun show(@PathVariable petId: Long, name: String?, page: Int, pet: Pet): String = name ?: ""
            }
            """.trimIndent()
        )
    }

    private fun addNestedApplication() {
        myFixture.addFileToProject(
            "com/example/app/PetClinicTests.java", """
            package com.example.app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.stereotype.Component;

            public class PetClinicTests {
                @SpringBootApplication
                public static class TestConfiguration {}

                @Component("clock")
                public static class Clock {}
            }
            """.trimIndent()
        )
    }

    private companion object {
        const val HTTP_SERVLET_REQUEST = "jakarta.servlet.http.HttpServletRequest"
        const val PUSH_BUILDER = "jakarta.servlet.http.PushBuilder"
        const val JAKARTA_PART = "jakarta.servlet.http.Part"
        const val MULTIPART_FILE = "org.springframework.web.multipart.MultipartFile"
        const val MULTIPART_REQUEST = "org.springframework.web.multipart.MultipartRequest"
        const val MULTIPART_HTTP_SERVLET_REQUEST = "org.springframework.web.multipart.MultipartHttpServletRequest"
        const val URI_COMPONENTS_BUILDER = "org.springframework.web.util.UriComponentsBuilder"
        const val SERVLET_URI_COMPONENTS_BUILDER = "org.springframework.web.servlet.support.ServletUriComponentsBuilder"
        const val SESSION_STATUS = "org.springframework.web.bind.support.SessionStatus"
    }
}
