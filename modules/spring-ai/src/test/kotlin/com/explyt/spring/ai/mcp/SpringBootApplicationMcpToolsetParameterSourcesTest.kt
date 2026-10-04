/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.beans.SpringBeanMcpToolset
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
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
}
