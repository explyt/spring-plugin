/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.codeInsight.daemon.LineMarkerInfo.LineMarkerGutterIconRenderer

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
        val endpoint = myFixture.addFileToProject(
            "CustomEndpoint.kt",
            "import org.springframework.boot.actuate.endpoint.annotation.Endpoint\n" +
                "import org.springframework.boot.actuate.endpoint.annotation.ReadOperation\n" +
                "@Endpoint(id = \"custom\") class CustomEndpoint {\n" +
                " @ReadOperation fun read(): String = \"custom\"\n}"
        )
        assertEquals(
            1,
            SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints(module)
                .count { it.type == EndpointType.ACTUATOR && it.path == "/actuator/custom" }
        )
        myFixture.configureByText(
            "CustomEndpointTest.kt",
            "import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get\n" +
                "fun request() = get(\"/actuator/custom\")"
        )
        myFixture.doHighlighting()

        val markers = myFixture.findAllGutters()
            .filter { it.tooltipText == "Endpoint Actions" }
            .mapNotNull { it as? LineMarkerGutterIconRenderer<*> }

        assertEquals(1, markers.size)
        assertEquals(endpoint.findElementAt(endpoint.text.indexOf("read"))?.text, markers.single().lineMarkerInfo.element?.text)
    }
}
