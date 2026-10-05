/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import kotlinx.coroutines.runBlocking

/**
 * Property names and nullability in `explyt_get_spring_endpoint_contract` as Jackson writes them: the naming strategy
 * a project declares renames every property `@JsonProperty` does not, a nullability the code does not state is
 * reported as unknown rather than as a guarantee, and a mapper the application configures in code makes the strategy
 * unknown rather than silently default.
 */
class SpringBootApplicationMcpToolsetJacksonNamingTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
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

    fun testObjectMapperBeanMakesTheStrategyUnknown() = runBlocking<Unit> {
        addJacksonConfig(
            """
            @Configuration
            class JacksonConfig {
                @Bean
                fun objectMapper(): ObjectMapper = ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals("@Bean objectMapper in com.example.config.JacksonConfig", schema["namingStrategySource"].asText())
        assertEquals(listOf("routingEnabled"), schema["fields"].map { it["name"].asText() })
        assertFalse(field(schema, "routingEnabled").has("declaredName"))
    }

    fun testBuilderCustomizerClassMakesTheStrategyUnknown() = runBlocking<Unit> {
        assertResolves(BOOT_3_CUSTOMIZER)
        addJacksonConfig(
            """
            @Component
            class SnakeCaseCustomizer : Jackson2ObjectMapperBuilderCustomizer {
                override fun customize(builder: Jackson2ObjectMapperBuilder) {
                    builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                }
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals(
            "com.example.config.SnakeCaseCustomizer implements $BOOT_3_CUSTOMIZER",
            schema["namingStrategySource"].asText()
        )
        assertEquals(listOf("routingEnabled"), schema["fields"].map { it["name"].asText() })
    }

    fun testBuilderCustomizerBeanMakesTheStrategyUnknown() = runBlocking<Unit> {
        assertResolves(BOOT_3_CUSTOMIZER)
        addJacksonConfig(
            """
            @Configuration
            class JacksonConfig {
                @Bean
                fun customizer(): Jackson2ObjectMapperBuilderCustomizer =
                    Jackson2ObjectMapperBuilderCustomizer { it.indentOutput(true) }
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals("@Bean customizer in com.example.config.JacksonConfig", schema["namingStrategySource"].asText())
    }

    fun testMapperBuilderBeanMakesTheStrategyUnknown() = runBlocking<Unit> {
        assertResolves("org.springframework.http.converter.json.Jackson2ObjectMapperBuilder")
        addJacksonConfig(
            """
            @Configuration
            class JacksonConfig {
                @Bean
                fun builder(): Jackson2ObjectMapperBuilder = Jackson2ObjectMapperBuilder()
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals("@Bean builder in com.example.config.JacksonConfig", schema["namingStrategySource"].asText())
    }

    fun testJavaObjectMapperBeanMakesTheStrategyUnknown() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/config/JavaJacksonConfig.java", """
            package com.example.config;

            import com.fasterxml.jackson.databind.ObjectMapper;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class JavaJacksonConfig {
                @Bean
                public ObjectMapper objectMapper() { return new ObjectMapper(); }
            }
            """.trimIndent()
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals("@Bean objectMapper in com.example.config.JavaJacksonConfig", schema["namingStrategySource"].asText())
    }

    fun testInferredReturnTypeOfAMapperBeanIsRead() = runBlocking<Unit> {
        addJacksonConfig(
            """
            @Configuration
            class JacksonConfig {
                @Bean
                fun objectMapper() = ObjectMapper()
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals("@Bean objectMapper in com.example.config.JacksonConfig", schema["namingStrategySource"].asText())
    }

    fun testAnonymousCustomizerOutsideABeanIsNotASource() = runBlocking<Unit> {
        assertResolves(BOOT_3_CUSTOMIZER)
        myFixture.addFileToProject(
            "com/example/config/Wiring.java", """
            package com.example.config;

            import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
            import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

            public class Wiring {
                Jackson2ObjectMapperBuilderCustomizer customizer() {
                    return new Jackson2ObjectMapperBuilderCustomizer() {
                        @Override public void customize(Jackson2ObjectMapperBuilder builder) { builder.indentOutput(true); }
                    };
                }
            }
            """.trimIndent()
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals(setOf("className", "fields"), schema.fieldNames().asSequence().toSet())
    }

    fun testMapperBeanInATestSourceRootIsNotASource() = runBlocking<Unit> {
        val testRoot = myFixture.tempDirFixture.findOrCreateDir("src/test/kotlin")
        PsiTestUtil.addSourceRoot(module, testRoot, true)
        try {
            IndexingTestUtil.waitUntilIndexesAreReady(project)
            myFixture.addFileToProject(
                "src/test/kotlin/com/example/config/TestJacksonConfig.kt", """
                package com.example.config

                import com.fasterxml.jackson.databind.ObjectMapper
                import org.springframework.boot.test.context.TestConfiguration
                import org.springframework.context.annotation.Bean

                @TestConfiguration
                class TestJacksonConfig {
                    @Bean
                    fun objectMapper(): ObjectMapper = ObjectMapper()
                }
                """.trimIndent()
            )
            myFixture.addFileToProject(
                "src/test/kotlin/org/springframework/boot/test/context/TestConfiguration.kt",
                "package org.springframework.boot.test.context\n\nannotation class TestConfiguration\n"
            )
            addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

            val schema = responseSchema()

            assertEquals(setOf("className", "fields"), schema.fieldNames().asSequence().toSet())
        } finally {
            PsiTestUtil.removeSourceRoot(module, testRoot)
        }
    }

    fun testAMapperBeanIsNamedBeforeACustomizerClass() = runBlocking<Unit> {
        assertResolves(BOOT_3_CUSTOMIZER)
        addJacksonConfig(
            """
            @Component
            class AaCustomizer : Jackson2ObjectMapperBuilderCustomizer {
                override fun customize(builder: Jackson2ObjectMapperBuilder) {}
            }

            @Configuration
            class ZzConfig {
                @Bean
                fun objectMapper(): ObjectMapper = ObjectMapper()
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals("@Bean objectMapper in com.example.config.ZzConfig", schema["namingStrategySource"].asText())
    }

    fun testJsonNamingOutranksAnObjectMapperBean() = runBlocking<Unit> {
        addJacksonConfig(
            """
            @Configuration
            class JacksonConfig {
                @Bean
                fun objectMapper(): ObjectMapper = ObjectMapper()
            }
            """
        )
        addController(
            "RouteSettings",
            """
            @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
            data class RouteSettings(val routingEnabled: Boolean)
            """
        )

        val schema = responseSchema()

        assertEquals("SNAKE_CASE", schema["namingStrategy"].asText())
        assertEquals("@JsonNaming", schema["namingStrategySource"].asText())
        assertEquals(listOf("routing_enabled"), schema["fields"].map { it["name"].asText() })
    }

    fun testObjectMapperBeanOutranksThePropertyAndKeepsItVisible() = runBlocking<Unit> {
        myFixture.addFileToProject("application.yml", "spring:\n  jackson:\n    property-naming-strategy: SNAKE_CASE\n")
        addJacksonConfig(
            """
            @Configuration
            class JacksonConfig {
                @Bean
                fun objectMapper(): ObjectMapper = ObjectMapper()
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals("UNKNOWN", schema["namingStrategy"].asText())
        assertEquals(
            "@Bean objectMapper in com.example.config.JacksonConfig, which may replace or override " +
                    "spring.jackson.property-naming-strategy=SNAKE_CASE",
            schema["namingStrategySource"].asText()
        )
        assertEquals(listOf("routingEnabled"), schema["fields"].map { it["name"].asText() })
    }

    fun testPlainDtoWithoutABeanOrAPropertyCarriesNoNamingKeys() = runBlocking<Unit> {
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals(setOf("className", "fields"), schema.fieldNames().asSequence().toSet())
        assertEquals(listOf("routingEnabled"), schema["fields"].map { it["name"].asText() })
    }

    fun testUnrelatedBeanLeavesTheNamingKeysAbsent() = runBlocking<Unit> {
        addJacksonConfig(
            """
            class Ledger

            @Configuration
            class LedgerConfig {
                @Bean
                fun ledger(): Ledger = Ledger()
            }
            """
        )
        addController("RouteSettings", "data class RouteSettings(val routingEnabled: Boolean)")

        val schema = responseSchema()

        assertEquals(setOf("className", "fields"), schema.fieldNames().asSequence().toSet())
    }

    fun testNestedClassUnderAnObjectMapperBeanIsUnknownToo() = runBlocking<Unit> {
        addJacksonConfig(
            """
            @Configuration
            class JacksonConfig {
                @Bean
                fun objectMapper(): ObjectMapper = ObjectMapper()
            }
            """
        )
        addController(
            "RouteSettings",
            """
            data class RouteSettings(val inner: Inner)
            data class Inner(val retryCount: Int)
            """
        )

        val nested = field(responseSchema(), "inner")["nested"]

        assertEquals("UNKNOWN", nested["namingStrategy"].asText())
        assertEquals("@Bean objectMapper in com.example.config.JacksonConfig", nested["namingStrategySource"].asText())
        assertEquals(listOf("retryCount"), nested["fields"].map { it["name"].asText() })
    }

    private fun addJacksonConfig(declarations: String) {
        myFixture.addFileToProject(
            "com/example/config/JacksonConfig.kt", """
            package com.example.config

            import com.fasterxml.jackson.databind.ObjectMapper
            import com.fasterxml.jackson.databind.PropertyNamingStrategies
            import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
            import org.springframework.stereotype.Component

            ${declarations.trimIndent()}
            """.trimIndent()
        )
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

    private fun assertResolves(fqn: String) {
        assertNotNull(
            "the fixture must resolve $fqn, or the test proves nothing",
            JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        )
    }

    private companion object {
        const val BOOT_3_CUSTOMIZER = "org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer"
    }
}
