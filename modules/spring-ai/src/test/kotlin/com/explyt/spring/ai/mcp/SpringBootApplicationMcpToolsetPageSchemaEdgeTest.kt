/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.util.WebApplicationStack
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.readAction
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PsiTestUtil
import kotlinx.coroutines.runBlocking

/**
 * Edges of the declared `PagedModel` return and of a composed `@EnableSpringDataWebSupport`.
 *
 * Only Spring Data's `org.springframework.data.web.PagedModel` is written as `content` + `page`; Spring HATEOAS's
 * `PagedModel` and a project class of the same simple name are other types. A blank serialization mode binds as no
 * value at all: Boot's `LenientObjectToEnumConverterFactory` trims the text and converts an empty one to `null`, and
 * `JavaBeanBinder` then leaves `SpringDataWebProperties.Pageable.serializationMode` at its `DIRECT` initializer.
 */
class SpringBootApplicationMcpToolsetPageSchemaEdgeTest : PageSchemaTestCase() {

    override val stack = WebApplicationStack.SERVLET

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_4_0,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
        TestLibrary.kotlin_1_9_22,
        TestLibrary(HATEOAS_COORDINATES, false),
    )

    fun testHateoasPagedModelIsNotGivenTheSpringDataShape() = runBlocking<Unit> {
        addController(
            "HateoasController", """
            @GetMapping("/orders/hateoas")
            public org.springframework.hateoas.PagedModel<OrderDto> hateoas() { return null; }
            """
        )

        assertNotTheSpringDataShape(schemaOf("/orders/hateoas", "$HATEOAS_PAGED_MODEL<$ORDER_DTO>"))
    }

    fun testKotlinHateoasPagedModelIsNotGivenTheSpringDataShape() = runBlocking<Unit> {
        addFile(
            "com/example/shop/KotlinHateoasController.kt", """
            package com.example.shop

            import org.springframework.hateoas.PagedModel
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class KotlinHateoasController {
                @GetMapping("/orders/kotlin-hateoas")
                fun hateoas(): PagedModel<OrderDto> = TODO()
            }
            """.trimIndent()
        )

        assertNotTheSpringDataShape(schemaOf("/orders/kotlin-hateoas", "$HATEOAS_PAGED_MODEL<$ORDER_DTO>"))
    }

    fun testProjectClassNamedPagedModelIsDescribedByItsOwnFields() = runBlocking<Unit> {
        addFile(
            "com/example/web/PagedModel.java", """
            package com.example.web;

            import java.util.List;

            public class PagedModel<T> {
                public List<T> getItems() { return null; }
                public String getCursor() { return null; }
            }
            """.trimIndent()
        )
        assertResolves(PROJECT_PAGED_MODEL)
        addController(
            "ProjectPagedModelController", """
            @GetMapping("/orders/project-model")
            public com.example.web.PagedModel<OrderDto> projectModel() { return null; }
            """
        )

        val schema = schemaOf("/orders/project-model", "$PROJECT_PAGED_MODEL<$ORDER_DTO>")

        assertEquals(schema.toString(), PROJECT_PAGED_MODEL, schema["className"].asText())
        assertEquals(schema.toString(), setOf("items", "cursor"), names(schema).toSet())
        assertNull(schema.toString(), schema["wireShape"])
        assertNull(schema.toString(), schema["wireShapeSource"])
    }

    fun testRawPagedModelKeepsTheShapeWithAnUntypedContent() = runBlocking<Unit> {
        addController(
            "RawPagedModelController", """
            @GetMapping("/orders/raw-model")
            @SuppressWarnings("rawtypes")
            public PagedModel rawModel() { return null; }
            """
        )

        val schema = schemaOf("/orders/raw-model", PAGED_MODEL)

        assertEquals(schema.toString(), PAGED_MODEL, schema["className"].asText())
        assertEquals("PAGED_MODEL", schema["wireShape"]?.asText())
        assertSource("DECLARED_TYPE", schema)
        assertEquals(listOf("content", "page"), names(schema))
        val content = field(schema, "content")
        assertEquals("java.util.List", content["type"].asText())
        assertTrue("a raw page has no element schema: $content", content["nested"].isNull)
        assertNull("a declared PagedModel is not an assumption: $schema", schema["wireShapeAssumed"])
    }

    fun testPagedModelsInsideAListKeepTheirDeclaredShape() = runBlocking<Unit> {
        addController(
            "PagedModelListController", """
            @GetMapping("/orders/models")
            public java.util.List<PagedModel<OrderDto>> models() { return null; }
            """
        )

        val schema = schemaOf("/orders/models", "java.util.List<$PAGED_MODEL<$ORDER_DTO>>")

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testKotlinPagedModelsInsideAListKeepTheirDeclaredShape() = runBlocking<Unit> {
        addFile(
            "com/example/shop/KotlinPagedModelListController.kt", """
            package com.example.shop

            import org.springframework.data.web.PagedModel
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class KotlinPagedModelListController {
                @GetMapping("/orders/kotlin-models")
                fun models(): List<PagedModel<OrderDto>> = emptyList()
            }
            """.trimIndent()
        )

        val schema = schemaOf("/orders/kotlin-models", "java.util.List<$PAGED_MODEL<$ORDER_DTO>>")

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testWhitespaceSerializationModeUsesBootDirectDefault() = runBlocking<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=   ")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testPlaceholderWithAnEmptyDefaultUsesBootDirectDefault() = runBlocking<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=\${PAGE_MODE:}")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testAliasedModeOnTheUsingClassOutranksTheMetaDefaultViaDto() = runBlocking<Unit> {
        addAliasedWebSupport(metaMode = "DIRECT")
        addAliasedWebConfig("ComposedWebConfig", "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
    }

    fun testAliasedModeOnTheUsingClassOutranksTheMetaDefaultDirect() = runBlocking<Unit> {
        addAliasedWebSupport(metaMode = "VIA_DTO")
        addAliasedWebConfig("ComposedWebConfig", "DIRECT")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
    }

    fun testAliasedModeSetFromKotlinOutranksTheMetaDefault() = runBlocking<Unit> {
        addAliasedWebSupport(metaMode = "DIRECT")
        addFile(
            "com/example/config/KotlinComposedWebConfig.kt", """
            package com.example.config

            import org.springframework.context.annotation.Configuration
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode

            @Configuration
            @EnableAliasedWeb(mode = PageSerializationMode.VIA_DTO)
            class KotlinComposedWebConfig
            """.trimIndent()
        )
        assertResolves("com.example.config.KotlinComposedWebConfig")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.KotlinComposedWebConfig", schema)
    }

    fun testComposedViaDtoUseWinsOverAComposedDirectUse() = runBlocking<Unit> {
        addAliasedWebSupport(metaMode = "DIRECT")
        addAliasedWebConfig("AlphaWebConfig", "DIRECT")
        addAliasedWebConfig("ZuluWebConfig", "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ZuluWebConfig", schema)
    }

    fun testComposedAnnotationUsedByNoClassHasNoEffect() = runBlocking<Unit> {
        addAliasedWebSupport(metaMode = "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testComposedAnnotationUsedOnlyInTestSourcesHasNoEffect() = runBlocking<Unit> {
        addAliasedWebSupport(metaMode = "VIA_DTO")
        val testRoot = myFixture.tempDirFixture.findOrCreateDir("src/test/java")
        PsiTestUtil.addSourceRoot(module, testRoot, true)
        try {
            addFile(
                "src/test/java/com/example/config/TestComposedWebConfig.java", """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;

                @Configuration
                @EnableAliasedWeb
                public class TestComposedWebConfig {
                }
                """.trimIndent()
            )
            assertResolves("com.example.config.TestComposedWebConfig")

            val schema = pageSchema("/orders/page")

            assertDirectPage(schema)
            assertSource("DEFAULT", schema)
        } finally {
            PsiTestUtil.removeSourceRoot(module, testRoot)
        }
    }

    private fun assertNotTheSpringDataShape(schema: JsonNode) {
        assertResolves(HATEOAS_PAGED_MODEL)
        assertResolves(PAGED_MODEL)
        assertFalse(schema.toString(), schema["className"]?.asText() == PAGED_MODEL)
        assertNull("HATEOAS writes _embedded/_links, not the Spring Data shape: $schema", schema["wireShape"])
        assertNull(schema.toString(), schema["wireShapeSource"])
        assertFalse("no Spring Data page metadata: $schema", "page" in names(schema))
    }

    private fun addController(className: String, methods: String) {
        addFile(
            "com/example/shop/$className.java", """
            package com.example.shop;

            import org.springframework.data.web.PagedModel;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class $className {
            ${methods.trimIndent()}
            }
            """.trimIndent()
        )
    }

    private fun addAliasedWebSupport(metaMode: String) {
        addFile("com/example/config/EnableAliasedWeb.java", aliasedWebSupport(metaMode))
        assertResolves(ALIASED_WEB_SUPPORT)
        assertResolves("org.springframework.core.annotation.AliasFor")
    }

    private fun addAliasedWebConfig(className: String, mode: String) {
        addFile("com/example/config/$className.java", aliasedWebConfig(className, mode))
        assertResolves("com.example.config.$className")
    }

    private companion object {
        const val HATEOAS_COORDINATES = "org.springframework.hateoas:spring-hateoas:2.2.0"
        const val HATEOAS_PAGED_MODEL = "org.springframework.hateoas.PagedModel"
        const val PROJECT_PAGED_MODEL = "com.example.web.PagedModel"
    }
}

/**
 * A composed `@EnableSpringDataWebSupport` declared in a dependency module and used by the application module. The
 * annotation type is on the application's compile classpath, so the using class decides the mode as a direct
 * declaration would.
 */
class SpringBootApplicationMcpToolsetPageSchemaModulesTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_4_0,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testComposedAnnotationFromADependencyModuleControlsTheApplicationMode() = runBlocking<Unit> {
        val shared = addDependencyModule("shared")
        addFileToModule(shared, "com/example/config/EnableAliasedWeb.java", aliasedWebSupport("DIRECT"))
        addFileToModule(module, "com/example/shop/ShopApplication.java", SHOP_APPLICATION)
        addFileToModule(module, "com/example/shop/OrderDto.java", ORDER_DTO_SOURCE)
        addFileToModule(module, "com/example/shop/OrderController.java", PAGE_CONTROLLER)
        addFileToModule(module, "com/example/config/ComposedWebConfig.java", aliasedWebConfig("ComposedWebConfig", "VIA_DTO"))

        assertEquals(WebApplicationStack.SERVLET, readAction { WebApplicationStack.of(module) })
        val annotation = readAction {
            JavaPsiFacade.getInstance(project).findClass(ALIASED_WEB_SUPPORT, GlobalSearchScope.moduleScope(shared))
        }
        assertNotNull("the composed annotation must live in the dependency module", annotation)

        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = "/orders/page", projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals(endpoints.toString(), 1, endpoints.size())
        val schema = endpoints.single()["responseSchema"]

        assertEquals(schema.toString(), "org.springframework.data.web.PagedModel", schema["className"]?.asText())
        assertEquals(schema.toString(), "PAGED_MODEL", schema["wireShape"]?.asText())
        assertEquals(
            schema.toString(),
            "@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig",
            schema["wireShapeSource"]?.asText()
        )
    }

    private companion object {
        val SHOP_APPLICATION = """
            package com.example.shop;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ShopApplication {
            }
        """.trimIndent()

        val ORDER_DTO_SOURCE = """
            package com.example.shop;

            public class OrderDto {
                public long getId() { return 0L; }
            }
        """.trimIndent()

        val PAGE_CONTROLLER = """
            package com.example.shop;

            import org.springframework.data.domain.Page;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OrderController {
                @GetMapping("/orders/page")
                public Page<OrderDto> page() { return null; }
            }
        """.trimIndent()
    }
}

private const val ALIASED_WEB_SUPPORT = "com.example.config.EnableAliasedWeb"

private fun aliasedWebSupport(metaMode: String): String = """
    package com.example.config;

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;
    import org.springframework.core.annotation.AliasFor;
    import org.springframework.data.web.config.EnableSpringDataWebSupport;
    import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.$metaMode)
    public @interface EnableAliasedWeb {
        @AliasFor(annotation = EnableSpringDataWebSupport.class, attribute = "pageSerializationMode")
        PageSerializationMode mode() default PageSerializationMode.$metaMode;
    }
""".trimIndent()

private fun aliasedWebConfig(className: String, mode: String): String = """
    package com.example.config;

    import org.springframework.context.annotation.Configuration;
    import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

    @Configuration
    @EnableAliasedWeb(mode = PageSerializationMode.$mode)
    public class $className {
    }
""".trimIndent()
