/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.references.ExplytControllerMethodReference
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.psi.PsiMethod

class ActuatorUrlReferenceTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
        TestLibrary.springReactiveWeb_3_1_1
    )

    fun testMockMvcHealthResolvesToHealthOperation() {
        addApplication()
        assertEndpoint("/actuator/health", "health")
        val reference = reference(
            "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                "class ActuatorTest { void request() { get(\"/actuator/he<caret>alth\"); } }"
        )
        assertEquals("health", (reference.resolve() as? PsiMethod)?.name)
    }

    fun testProjectEndpointResolvesFromMockMvc() {
        addApplication()
        addCustomEndpoint()
        assertEndpoint("/actuator/custom", "read")
        val reference = reference(
            "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                "class ActuatorTest { void request() { get(\"/actuator/cus<caret>tom\"); } }"
        )
        assertEquals("read", (reference.resolve() as? PsiMethod)?.name)
    }

    fun testDeclaredBasePathIsUsed() {
        addApplication()
        addCustomEndpoint()
        myFixture.addFileToProject("application.properties", "management.endpoints.web.base-path=/manage")
        assertEndpoint("/manage/custom", "read")
        val matching = reference(
            "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                "class ActuatorTest { void request() { get(\"/manage/cus<caret>tom\"); } }"
        )
        assertEquals("read", (matching.resolve() as? PsiMethod)?.name)
        val nonMatching = reference(
            "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                "class ActuatorTest { void request() { get(\"/actuator/cus<caret>tom\"); } }"
        )
        assertNull(nonMatching.resolve())
    }

    fun testUnknownActuatorPathRemainsUnresolved() {
        addApplication()
        assertTrue(
            SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
                .none { it.type == EndpointType.ACTUATOR && it.path == "/actuator/nothing" }
        )
        val reference = reference(
            "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;\n" +
                "class ActuatorTest { void request() { get(\"/actuator/not<caret>hing\"); } }"
        )
        assertNull(reference.resolve())
    }

    private fun reference(text: String): ExplytControllerMethodReference {
        myFixture.configureByText("ActuatorTest.java", text)
        return file.findReferenceAt(myFixture.caretOffset) as ExplytControllerMethodReference
    }

    private fun assertEndpoint(path: String, operation: String) {
        val endpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.ACTUATOR && it.path == path }
        assertEquals(1, endpoints.size)
        assertEquals(operation, (endpoints.single().psiElement as PsiMethod).name)
    }

    private fun addApplication() {
        myFixture.addFileToProject(
            "Application.java",
            "import org.springframework.boot.autoconfigure.SpringBootApplication;\n" +
                "@SpringBootApplication class Application {}"
        )
    }

    private fun addCustomEndpoint() {
        myFixture.addFileToProject(
            "CustomEndpoint.java",
            "import org.springframework.boot.actuate.endpoint.annotation.Endpoint;\n" +
                "import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;\n" +
                "@Endpoint(id = \"custom\") class CustomEndpoint {\n" +
                " @ReadOperation String read() { return \"custom\"; }\n}"
        )
    }
}
