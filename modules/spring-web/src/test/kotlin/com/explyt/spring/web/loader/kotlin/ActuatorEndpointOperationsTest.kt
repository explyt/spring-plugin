/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.view.EndpointsTreeData
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

/**
 * What an Actuator endpoint answers, operation by operation: the verbs and paths its mapping methods declare when it
 * is a controller endpoint, the media types an operation produces, and no invented verb when it declares neither.
 */
class ActuatorEndpointOperationsTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springWeb_6_1_4,
    )

    override fun setUp() {
        super.setUp()
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: @RestControllerEndpoint is on the classpath",
            facade.findClass("org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint", scope)
        )
        assertNotNull(
            "precondition: @GetMapping is on the classpath",
            facade.findClass("org.springframework.web.bind.annotation.GetMapping", scope)
        )
    }

    fun testRestControllerEndpointListsEachMappingWithItsVerbAndMethod() {
        myFixture.addFileToProject(
            "CustomEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PostMapping

            @RestControllerEndpoint(id = "custom")
            class CustomEndpoint {
                @GetMapping("/items")
                fun items(): List<String> = emptyList()

                @PostMapping("/items")
                fun add(): String = "added"
            }
            """.trimIndent()
        )

        val endpoints = endpointsOf("CustomEndpoint")

        assertEquals(
            listOf(
                Triple("/actuator/custom/items", listOf("GET"), "items"),
                Triple("/actuator/custom/items", listOf("POST"), "add"),
            ),
            endpoints.map { Triple(it.path, it.requestMethods, (it.psiElement as? PsiMethod)?.name) }
                .sortedBy { it.second.joinToString() }
        )
    }

    /** A bare `@RequestMapping` accepts any verb, which the endpoint model writes as no verb restriction. */
    fun testControllerEndpointWithABareRequestMappingAcceptsAnyVerb() {
        myFixture.addFileToProject(
            "LegacyEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint
            import org.springframework.web.bind.annotation.RequestMapping

            @ControllerEndpoint(id = "legacy")
            class LegacyEndpoint {
                @RequestMapping
                fun any(): String = "any"
            }
            """.trimIndent()
        )

        val endpoint = endpointsOf("LegacyEndpoint").single()

        assertEquals("/actuator/legacy", endpoint.path)
        assertEquals(emptyList<String>(), endpoint.requestMethods)
        assertEquals("any", (endpoint.psiElement as? PsiMethod)?.name)
    }

    fun testProjectOperationCarriesTheMediaTypesItProduces() {
        myFixture.addFileToProject(
            "ReportEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint(id = "report")
            class ReportEndpoint {
                @ReadOperation(produces = ["text/plain"])
                fun text(): String = "report"
            }
            """.trimIndent()
        )

        val endpoint = endpointsOf("ReportEndpoint").single()

        assertEquals(listOf("GET"), endpoint.requestMethods)
        assertEquals(listOf("text/plain"), endpoint.produces)
    }

    fun testBuiltInTextThreadDumpCarriesItsMediaType() {
        val threadDump = endpointsOf("ThreadDumpEndpoint")
        val text = threadDump.single { (it.psiElement as? PsiMethod)?.name == "textThreadDump" }
        val json = threadDump.single { (it.psiElement as? PsiMethod)?.name == "threadDump" }

        assertTrue("textThreadDump produces text/plain, got ${text.produces}", text.produces.any { it.startsWith("text/plain") })
        assertEquals("an operation declaring no media type carries none", emptyList<String>(), json.produces)
    }

    /** An endpoint declaring nothing it answers keeps its row, but no verb is invented for it. */
    fun testEndpointWithoutOperationsHasNoInventedVerb() {
        myFixture.addFileToProject(
            "EmptyEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint

            @Endpoint(id = "empty")
            class EmptyEndpoint
            """.trimIndent()
        )

        val endpoint = endpointsOf("EmptyEndpoint").single()

        assertEquals("/actuator/empty", endpoint.path)
        assertEquals(emptyList<String>(), endpoint.requestMethods)
        assertFalse(
            "no element names its endpoint type as a verb",
            allActuatorEndpoints().any { EndpointType.ACTUATOR.name in it.requestMethods }
        )
        assertEquals("the tool window keeps one unnamed row for it", listOf(""), EndpointsTreeData.rowVerbsOf(endpoint))
    }

    private fun endpointsOf(className: String): List<EndpointElement> =
        allActuatorEndpoints().filter { it.containingClass?.name == className }

    private fun allActuatorEndpoints(): List<EndpointElement> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
}
