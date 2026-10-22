/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.builder.openapi

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.builder.openapi.json.OpenApiJsonPathHttpTypeBuilder
import com.explyt.spring.web.builder.openapi.yaml.OpenApiYamlPathHttpTypeBuilder
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.util.SpringWebUtil
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement

/**
 * What "Add endpoint to OpenAPI" states about whether a client has to send a query parameter or a header.
 *
 * Spring binds a missing value to a parameter with a `defaultValue`, and to one it considers optional - `Optional`,
 * `@Nullable`, a Kotlin nullable type or a Kotlin default value - instead of rejecting the request. The generated
 * specification said `required: true` for all of them, next to the very `default` that makes them optional, so a
 * client generated from it made the parameter mandatory.
 */
class OpenApiParameterRequirednessTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
    )

    fun testJavaParameterWithADefaultOrOptionalTypeIsNotRequired() {
        myFixture.addFileToProject(
            "com/example/WindowController.java", """
            package com.example;

            import java.util.Optional;
            import org.springframework.lang.Nullable;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestHeader;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class WindowController {
                @GetMapping("/api/activity")
                public String activity(
                        @RequestParam(defaultValue = "7d") String window,
                        @RequestParam(value = "from", required = true, defaultValue = "0") String from,
                        @RequestParam @Nullable String cursor,
                        @RequestParam Optional<String> sort,
                        @RequestParam String page,
                        @RequestHeader(value = "X-Trace", defaultValue = "none") String trace,
                        @RequestHeader("X-Tenant") String tenant) {
                    return window;
                }
            }
            """.trimIndent()
        )

        val expected = mapOf(
            "window" to false, "from" to false, "cursor" to false, "sort" to false, "page" to true,
            "X-Trace" to false, "X-Tenant" to true,
        )
        val endpoint = endpointInfo("com.example.WindowController", "activity")

        assertEquals(expected, requirednessInYaml(endpoint))
        assertEquals(expected, requirednessInJson(endpoint))
        assertEquals("7d", jsonParameters(endpoint).getValue("window")["default"].asText())
    }

    fun testKotlinNullableOrDefaultedParameterIsNotRequired() {
        myFixture.addFileToProject(
            "com/example/ActivityController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class ActivityController {
                @GetMapping("/api/links/activity")
                fun activity(
                    @RequestParam(defaultValue = "7d") window: String,
                    @RequestParam from: String?,
                    @RequestParam size: Int = 20,
                    @RequestParam page: Int,
                ): String = window
            }
            """.trimIndent()
        )

        val expected = mapOf("window" to false, "from" to false, "size" to false, "page" to true)
        val endpoint = endpointInfo("com.example.ActivityController", "activity")

        assertEquals(expected, requirednessInYaml(endpoint))
        assertEquals(expected, requirednessInJson(endpoint))
    }

    private fun endpointInfo(className: String, methodName: String): EndpointInfo {
        val controller = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
            ?: error("$className is not in the fixture")
        val method = controller.findMethodsByName(methodName, false).single().toUElement() as UMethod
        return SpringWebUtil.getEndpointInfo(method) ?: error("$className.$methodName is not an endpoint")
    }

    /**
     * The `name`/`required` pairs of the generated YAML operation, read line by line because the builder emits a
     * fragment of a larger document rather than a document of its own.
     */
    private fun requirednessInYaml(endpoint: EndpointInfo): Map<String, Boolean> {
        val yaml = StringBuilder().also { OpenApiYamlPathHttpTypeBuilder(endpoint, GET, builder = it).build() }.toString()
        val lines = yaml.lines().map(String::trim)
        return lines.withIndex()
            .filter { (_, line) -> line.startsWith("- name: ") }
            .associate { (index, line) ->
                val required = lines.drop(index).first { it.startsWith("required: ") }
                line.removePrefix("- name: ") to required.removePrefix("required: ").toBooleanStrict()
            }
    }

    private fun requirednessInJson(endpoint: EndpointInfo): Map<String, Boolean> =
        jsonParameters(endpoint).mapValues { it.value["required"].asBoolean() }

    private fun jsonParameters(endpoint: EndpointInfo): Map<String, JsonNode> {
        val json = StringBuilder().also { OpenApiJsonPathHttpTypeBuilder(endpoint, GET, builder = it).build() }
        return ObjectMapper().readTree("{$json}")[GET.lowercase()]["parameters"].associateBy { it["name"].asText() }
    }

    private companion object {
        const val GET = "GET"
    }
}
