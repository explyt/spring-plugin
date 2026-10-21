/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.providers.EndpointRunLineMarkerProvider
import com.explyt.spring.web.providers.RunInSwaggerAction
import com.explyt.spring.web.util.OpenApiFileUtil
import org.jetbrains.kotlin.psi.KtNamedFunction

class ServerPortPlaceholderSwaggerServersTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
    )

    fun testNestedPlaceholderFallsBackToTheInnerDefault() {
        myFixture.addFileToProject(
            "application.properties",
            "server.port=\${SERVER_PORT:\${app.server.port:9317}}\n"
        )

        assertEquals(listOf("http://localhost:9317", "http://localhost:8080"), swaggerServers())
    }

    fun testNestedPlaceholderTakesTheInnerKeyValue() {
        myFixture.addFileToProject(
            "application.properties",
            "server.port=\${SERVER_PORT:\${app.server.port:9317}}\napp.server.port=9400\n"
        )

        assertEquals(listOf("http://localhost:9400", "http://localhost:8080"), swaggerServers())
    }

    fun testNestedPlaceholderInYaml() {
        myFixture.addFileToProject(
            "application.yml",
            "server:\n  port: \${SERVER_PORT:\${app.server.port:9317}}\n"
        )

        assertEquals(listOf("http://localhost:9317", "http://localhost:8080"), swaggerServers())
    }

    fun testPlainPort() {
        myFixture.addFileToProject("application.properties", "server.port=8081\n")

        assertEquals(listOf("http://localhost:8081", "http://localhost:8080"), swaggerServers())
    }

    fun testSingleLevelPlaceholderDefault() {
        myFixture.addFileToProject("application.properties", "server.port=\${server.port.custom:8082}\n")

        assertEquals(listOf("http://localhost:8082", "http://localhost:8080"), swaggerServers())
    }

    fun testNoServerPortMeansTheDefaultPort() {
        assertEquals(listOf("http://localhost:8080"), swaggerServers())
    }

    private fun swaggerServers(): List<String> {
        myFixture.addFileToProject(
            "com/example/PingController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class PingController {
                @GetMapping("/ping")
                fun ping(): String = "pong"
            }
            """.trimIndent()
        )
        val method = myFixture.findClass("com.example.PingController").findMethodsByName("ping", false).single()
        val identifier = (method.navigationElement as KtNamedFunction).nameIdentifier!!
        val action = EndpointRunLineMarkerProvider().getInfo(identifier)?.actions
            ?.filterIsInstance<RunInSwaggerAction>()?.single()
            ?: error("No Run in Swagger action on PingController.ping")
        return OpenApiFileUtil.INSTANCE.endpointServersOf(action.endpoints())
    }
}
