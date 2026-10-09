/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.completion.kotlin

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType

import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod

class ActuatorUrlReferenceMultiModuleTest : ExplytMultiModuleTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary.springBootActuatorAutoConfigure_4_1_0
    )

    fun testMockMvcFromTestModuleResolvesApplicationEndpoint() {
        addFileToModule(
            module,
            "Application.kt",
            "import org.springframework.boot.autoconfigure.SpringBootApplication\n@SpringBootApplication class Application"
        )
        addFileToModule(
            module,
            "CustomEndpoint.kt",
            "import org.springframework.boot.actuate.endpoint.annotation.Endpoint\n" +
                "import org.springframework.boot.actuate.endpoint.annotation.ReadOperation\n" +
                "@Endpoint(id = \"custom\") class CustomEndpoint {\n" +
                " @ReadOperation fun read(): String = \"custom\"\n}"
        )
        val appEndpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
            .filter { it.type == EndpointType.ACTUATOR && it.path == "/actuator/custom" }
        assertEquals(1, appEndpoints.size)
        assertEquals("read", (appEndpoints.single().psiElement as PsiMethod).name)

        val testModule = addDependentModule("app-test")
        val request = addFileToModule(
            testModule,
            "CustomEndpointTest.kt",
            "import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get\n" +
                "fun request() = get(\"/actuator/custom\")"
        )
        val reference = request.findReferenceAt(request.text.indexOf("custom") + 2)

        assertEquals("read", (reference?.resolve() as? PsiMethod)?.name)
        assertEquals(
            1,
            SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
                .count { it.type == EndpointType.ACTUATOR && it.path == "/actuator/custom" }
        )
    }
}
