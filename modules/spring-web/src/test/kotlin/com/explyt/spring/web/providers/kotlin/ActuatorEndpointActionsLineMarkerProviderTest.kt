/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.TestUtil.findTypedReferenceAt
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.providers.EndpointIconGutterHandler
import com.explyt.spring.web.providers.EndpointUsageSearcher
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer
import com.intellij.psi.PsiMethod

class ActuatorEndpointActionsLineMarkerProviderTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7
    )

    fun testProjectEndpointListsMockMvcUsage() {
        myFixture.addFileToProject(
            "Application.kt",
            "import org.springframework.boot.autoconfigure.SpringBootApplication\n@SpringBootApplication class Application"
        )
        val request = myFixture.addFileToProject(
            "CustomEndpointTest.kt",
            "import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get\n" +
                "class CustomEndpointTest {\n" +
                "    fun request() = get(\"/actuator/custom\")\n" +
                "}"
        )
        val requestReference = request.findTypedReferenceAt<ExplytControllerMethodReference>(
            request.text.indexOf("custom") + 2
        )
        assertNotNull("precondition: MockMvc URL reference exists", requestReference)

        val endpointFile = myFixture.configureByText(
            "CustomEndpoint.kt",
            "import org.springframework.boot.actuate.endpoint.annotation.Endpoint\n" +
                "import org.springframework.boot.actuate.endpoint.annotation.ReadOperation\n" +
                "@Endpoint(id = \"custom\") class CustomEndpoint {\n" +
                " @ReadOperation fun read(): String = \"custom\"\n" +
                " fun helper(): String = \"helper\"\n}"
        )
        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.ACTUATOR && it.path == "/actuator/custom" }
        assertEquals(1, endpoints.size)
        assertEquals("read", (endpoints.single().psiElement as PsiMethod).name)
        assertEquals("read", (requestReference!!.resolve() as? PsiMethod)?.name)

        myFixture.doHighlighting()
        val markers = myFixture.findAllGutters()
            .filter { it.tooltipText == "Endpoint Actions" }
            .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
        assertEquals(1, markers.size)
        assertEquals("read", markers.single().lineMarkerInfo.element?.text)
        assertFalse(markers.any { it.lineMarkerInfo.element?.text == "helper" })

        val handler = markers.single().lineMarkerInfo.navigationHandler as EndpointIconGutterHandler
        assertEquals("/actuator/custom", handler.endpointInfo.path)
        assertTrue(
            EndpointUsageSearcher.findTestRequestUsage(
                handler.endpointInfo.path,
                handler.endpointInfo.requestMethods,
                module
            ).any { it.text.contains("get(\"/actuator/custom\")") }
        )
        assertNotNull(endpointFile.findElementAt(endpointFile.text.indexOf("read")))
    }

    fun testWriteAndDeleteOperationsGetGuttersOnlyOnAnEndpoint() {
        myFixture.addFileToProject("Application.kt", "import org.springframework.boot.autoconfigure.SpringBootApplication\n@SpringBootApplication class Application")
        myFixture.configureByText(
            "Operations.kt",
            "import org.springframework.boot.actuate.endpoint.annotation.*\n" +
                "@Endpoint(id = \"operations\") class Operations {\n" +
                " @WriteOperation fun write(): String = \"write\"\n" +
                " @DeleteOperation fun delete(): String = \"delete\"\n" +
                " fun plain(): String = \"plain\"\n}\n" +
                "class NotAnEndpoint { @ReadOperation fun read(): String = \"read\" }"
        )
        myFixture.doHighlighting()
        val markers = myFixture.findAllGutters().filter { it.tooltipText == "Endpoint Actions" }
            .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }
        assertEquals(setOf("write", "delete"), markers.mapNotNull { it.lineMarkerInfo.element?.text }.toSet())
    }
}
