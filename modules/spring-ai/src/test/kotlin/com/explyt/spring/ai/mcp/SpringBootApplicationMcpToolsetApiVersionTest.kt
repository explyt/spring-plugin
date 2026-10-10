/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.util.WebApplicationStack
import kotlinx.coroutines.runBlocking

abstract class McpToolsetApiVersionTestCase : McpToolsetWebStackTestCase() {

    protected fun assertApiVersionStack(stack: WebApplicationStack) {
        assertEquals(stack, WebApplicationStack.of(module))
        assertResolving(API_VERSION, "java.util.Optional", "org.springframework.stereotype.Component")
    }

    protected fun addJavaVersionController() {
        myFixture.addFileToProject(
            "com/example/app/web/VersionController.java", """
            package com.example.app.web;

            import java.util.Optional;
            import org.springframework.web.accept.SemanticApiVersionParser;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class VersionController {
                @GetMapping("/api/version")
                public String version(SemanticApiVersionParser.Version version,
                                      Optional<SemanticApiVersionParser.Version> requested, String name) {
                    return name;
                }
            }
            """.trimIndent()
        )
    }

    protected fun addKotlinVersionController() {
        myFixture.addFileToProject(
            "com/example/app/web/KotlinVersionController.kt", """
            package com.example.app.web

            import org.springframework.web.accept.SemanticApiVersionParser
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class KotlinVersionController {
                @GetMapping("/api/kotlin-version")
                fun version(version: SemanticApiVersionParser.Version, name: String): String = name
            }
            """.trimIndent()
        )
    }

    protected companion object {
        const val API_VERSION = "org.springframework.web.accept.SemanticApiVersionParser.Version"
    }
}

class SpringBootApplicationMcpToolsetServletApiVersionTest : McpToolsetApiVersionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-webmvc:7.0.9"),
        TestLibrary.kotlin_1_9_22,
    )

    fun testApiVersionArgumentIsSuppliedByTheServletFramework() = runBlocking<Unit> {
        assertApiVersionStack(WebApplicationStack.SERVLET)
        addJavaVersionController()

        assertEquals(
            mapOf("version" to "FRAMEWORK", "requested" to "FRAMEWORK", "name" to "QUERY"),
            contractSourcesOf("/api/version"),
        )
    }

    fun testKotlinApiVersionArgumentIsSuppliedByTheServletFramework() = runBlocking<Unit> {
        assertApiVersionStack(WebApplicationStack.SERVLET)
        addKotlinVersionController()

        assertEquals(
            mapOf("version" to "FRAMEWORK", "name" to "QUERY"),
            contractSourcesOf("/api/kotlin-version"),
        )
    }
}

class SpringBootApplicationMcpToolsetReactiveApiVersionTest : McpToolsetApiVersionTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-webflux:7.0.9"),
        TestLibrary("org.springframework:spring-context:7.0.9"),
        TestLibrary.kotlin_1_9_22,
    )

    fun testApiVersionArgumentIsSuppliedByTheReactiveFramework() = runBlocking<Unit> {
        assertApiVersionStack(WebApplicationStack.REACTIVE)
        addJavaVersionController()

        assertEquals(
            mapOf("version" to "FRAMEWORK", "requested" to "FRAMEWORK", "name" to "QUERY"),
            contractSourcesOf("/api/version"),
        )
    }
}
