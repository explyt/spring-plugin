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
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

/**
 * The media types an Actuator operation answers with when it declares none, as Boot's `RequestPredicateFactory.getProduces`
 * decides them: the declared `produces` and `producesFrom` types first, nothing for a `void` operation,
 * `application/octet-stream` for a `Resource`, and otherwise the endpoint defaults of `EndpointMediaTypes.DEFAULT`.
 */
class ActuatorDefaultProducesTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0, TestLibrary.springBootHealth_4_1_0)

    override fun setUp() {
        super.setUp()
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: Producible is on the classpath",
            facade.findClass("org.springframework.boot.actuate.endpoint.Producible", scope)
        )
        assertNotNull(
            "precondition: Resource is on the classpath",
            facade.findClass("org.springframework.core.io.Resource", scope)
        )
    }

    fun testBuiltInHealthReadOperationProducesTheEndpointDefaults() {
        myFixture.addFileToProject("DemoApplication.kt", APPLICATION)

        val health = operation("HealthEndpoint", "health")

        assertEquals("/actuator/health", health.path)
        assertEquals(DEFAULT_PRODUCES, health.produces)
    }

    fun testProjectReadOperationReturningAnObjectProducesTheEndpointDefaults() {
        addEndpoint(
            """
            @ReadOperation
            fun report(): Report = Report("ok")
            """
        )

        assertEquals(DEFAULT_PRODUCES, operation("ReportEndpoint", "report").produces)
    }

    fun testDeclaredProducesReplacesTheDefaults() {
        addEndpoint(
            """
            @ReadOperation(produces = ["text/plain"])
            fun report(): String = "ok"
            """
        )

        assertEquals(listOf("text/plain"), operation("ReportEndpoint", "report").produces)
    }

    fun testVoidWriteOperationProducesNothing() {
        addEndpoint(
            """
            @WriteOperation
            fun reset(name: String) {
            }
            """
        )

        val reset = operation("ReportEndpoint", "reset")

        assertEquals(listOf("POST"), reset.requestMethods)
        assertEquals(emptyList<String>(), reset.produces)
    }

    fun testResourceReadOperationProducesAnOctetStream() {
        addEndpoint(
            """
            @ReadOperation
            fun download(): org.springframework.core.io.Resource = org.springframework.core.io.ByteArrayResource(ByteArray(0))
            """
        )

        assertEquals(listOf(OCTET_STREAM), operation("ReportEndpoint", "download").produces)
    }

    fun testWebEndpointResponseOfResourceProducesAnOctetStream() {
        addEndpoint(
            """
            @ReadOperation
            fun download(): org.springframework.boot.actuate.endpoint.web.WebEndpointResponse<org.springframework.core.io.Resource> =
                org.springframework.boot.actuate.endpoint.web.WebEndpointResponse(org.springframework.core.io.ByteArrayResource(ByteArray(0)))
            """
        )

        assertEquals(listOf(OCTET_STREAM), operation("ReportEndpoint", "download").produces)
    }

    fun testProducesFromListsEveryConstantOfTheProducibleEnum() {
        myFixture.addFileToProject(
            "ReportFormat.kt",
            """
            import org.springframework.boot.actuate.endpoint.Producible
            import org.springframework.util.MimeType

            enum class ReportFormat(private val mimeType: String) : Producible<ReportFormat> {
                CSV("text/csv"),
                MARKDOWN("text/markdown");

                override fun getProducedMimeType(): MimeType = MimeType.valueOf(mimeType)
            }
            """.trimIndent()
        )
        addEndpoint(
            """
            @ReadOperation(producesFrom = ReportFormat::class)
            fun report(format: ReportFormat): String = format.name
            """
        )

        assertEquals(listOf("text/csv", "text/markdown"), operation("ReportEndpoint", "report").produces)
    }

    private fun addEndpoint(operations: String) {
        myFixture.addFileToProject(
            "ReportEndpoint.kt",
            """
            |import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            |import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            |import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
            |
            |data class Report(val status: String)
            |
            |@Endpoint(id = "report")
            |class ReportEndpoint {
            |${operations.trimIndent().prependIndent("    ")}
            |}
            """.trimMargin()
        )
    }

    private fun operation(className: String, methodName: String): EndpointElement {
        val operations = actuatorEndpoints().filter {
            it.containingClass?.name == className && (it.psiElement as? PsiMethod)?.name == methodName
        }
        assertEquals("precondition: one $className.$methodName operation is listed", 1, operations.size)
        return operations.single()
    }

    private fun actuatorEndpoints(): List<EndpointElement> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
}

private const val OCTET_STREAM = "application/octet-stream"

private val DEFAULT_PRODUCES = listOf(
    "application/vnd.spring-boot.actuator.v3+json",
    "application/vnd.spring-boot.actuator.v2+json",
    "application/json",
)
