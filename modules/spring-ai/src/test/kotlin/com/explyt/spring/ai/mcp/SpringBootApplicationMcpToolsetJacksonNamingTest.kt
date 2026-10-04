/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * Property names and nullability in `explyt_get_spring_endpoint_contract` as Jackson writes them: the naming strategy
 * a project declares renames every property `@JsonProperty` does not, and a nullability the code does not state is
 * reported as unknown rather than as a guarantee.
 */
class SpringBootApplicationMcpToolsetJacksonNamingTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springContext_6_0_7,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testJsonNamingRenamesPropertiesAndIsReported() = runBlocking<Unit> {
        addController(
            "RouteSettings",
            """
            @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
            data class RouteSettings(val routingEnabled: Boolean, @JsonProperty("Max") val maxHops: Int, val inner: Inner)
            data class Inner(val retryCount: Int)
            """
        )

        val schema = responseSchema()

        assertEquals(listOf("routing_enabled", "Max", "inner"), schema["fields"].map { it["name"].asText() })
        assertEquals("SNAKE_CASE", schema["namingStrategy"].asText())
        assertEquals("@JsonNaming", schema["namingStrategySource"].asText())
        assertEquals("routingEnabled", field(schema, "routing_enabled")["declaredName"].asText())
        assertEquals(
            "@JsonNaming applies to its own class only",
            listOf("retryCount"), field(schema, "inner")["nested"]["fields"].map { it["name"].asText() }
        )
    }

    fun testInheritedJsonNamingApplies() = runBlocking<Unit> {
        addController(
            "RouteSettings",
            """
            @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy::class)
            open class Base
            class RouteSettings(val routingEnabled: Boolean) : Base()
            """
        )

        assertEquals(listOf("routing-enabled"), responseSchema()["fields"].map { it["name"].asText() })
    }

    fun testConfiguredStrategyRenamesNestedClassesAndAcronymsLikeJackson() = runBlocking<Unit> {
        myFixture.addFileToProject("application.yml", "spring:\n  jackson:\n    property-naming-strategy: SNAKE_CASE\n")
        addController(
            "RouteSettings",
            """
            data class RouteSettings(val URLValue: String, val inner: Inner)
            data class Inner(val retryCount: Int)
            """
        )

        val schema = responseSchema()

        assertEquals(listOf("urlvalue", "inner"), schema["fields"].map { it["name"].asText() })
        assertEquals("spring.jackson.property-naming-strategy", schema["namingStrategySource"].asText())
        assertEquals(listOf("retry_count"), field(schema, "inner")["nested"]["fields"].map { it["name"].asText() })
    }

    fun testCustomStrategyIsUnknownAndRenamesNothing() = runBlocking<Unit> {
        addController(
            "RouteSettings",
            """
            class Custom : PropertyNamingStrategies.NamingBase() { override fun translate(name: String) = name.reversed() }
            @JsonNaming(Custom::class)
            data class RouteSettings(val routingEnabled: Boolean)
            """
        )

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals(listOf("routingEnabled"), schema["fields"].map { it["name"].asText() })
    }

    fun testEnumValuesAreNotRenamed() = runBlocking<Unit> {
        addController(
            "RouteSettings",
            """
            @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
            data class RouteSettings(val routeMode: RouteMode)
            enum class RouteMode { FastPath, SlowPath }
            """
        )

        val mode = field(responseSchema(), "route_mode")
        assertEquals(listOf("FastPath", "SlowPath"), mode["nested"]["enumValues"].map { it.asText() })
    }

    fun testKotlinNullability() = runBlocking<Unit> {
        addController("RouteSettings", "data class RouteSettings(val count: Int, val limit: Int?, val name: String)")

        val schema = responseSchema()

        assertEquals(false, field(schema, "count")["nullable"].booleanValue())
        assertEquals(true, field(schema, "limit")["nullable"].booleanValue())
        assertEquals(false, field(schema, "name")["nullable"].booleanValue())
    }

    fun testJavaNullabilityIsUnknownWithoutAnAnnotation() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/web/Version.java", """
            package com.example.web;
            public class Version {
                @interface NotNull {}
                @interface Nullable {}
                public int primitive;
                @NotNull public Integer required;
                public Integer unannotated;
                @Nullable public Integer optional;
            }
            """.trimIndent()
        )
        addController("Version", "")

        val schema = responseSchema()

        assertEquals(false, field(schema, "primitive")["nullable"].booleanValue())
        assertEquals(false, field(schema, "required")["nullable"].booleanValue())
        assertTrue(
            "an unannotated Java reference is unknown, written as null, got ${field(schema, "unannotated")}",
            field(schema, "unannotated").has("nullable") && field(schema, "unannotated")["nullable"].isNull
        )
        assertEquals(true, field(schema, "optional")["nullable"].booleanValue())
    }

    private fun addController(returnType: String, declarations: String) {
        myFixture.addFileToProject(
            "com/example/web/RouteController.kt", """
            package com.example.web

            import com.fasterxml.jackson.annotation.JsonProperty
            import com.fasterxml.jackson.databind.PropertyNamingStrategies
            import com.fasterxml.jackson.databind.annotation.JsonNaming
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            ${declarations.trimIndent()}

            @RestController
            class RouteController {
                @GetMapping("/api/route-settings")
                fun settings(): $returnType = TODO()
            }
            """.trimIndent()
        )
    }

    private suspend fun responseSchema(): JsonNode {
        val contract = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/api/route-settings", projectPath = project.basePath ?: "")
        )
        val schema = contract["endpoints"].single()["responseSchema"]
        assertTrue("A response schema is expected, got $contract", schema != null && !schema.isNull)
        return schema
    }

    private fun field(schema: JsonNode, name: String): JsonNode =
        schema["fields"].single { it["name"].asText() == name }
}
