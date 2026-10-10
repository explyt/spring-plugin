/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.util.WebApplicationStack
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import kotlinx.coroutines.runBlocking

/**
 * A Spring Data `Page` or `Slice` response in `explyt_get_spring_endpoint_contract` is described as Spring Data writes
 * it, not by its type argument.
 *
 * `PageImpl` is written through its getters (`DIRECT`) unless `@EnableSpringDataWebSupport(pageSerializationMode =
 * VIA_DTO)` or, on a servlet application, `spring.data.web.pageable.serialization-mode` re-wraps it into `PagedModel`
 * (`content` + `page`). `SliceImpl` is never re-wrapped. The schema names the shape in `wireShape` and where the mode was
 * read in `wireShapeSource` - `UNKNOWN` when it cannot be read, never a confident guess - and `wireShapeAssumed` says
 * that the declared interface was taken for its Spring Data implementation.
 *
 * The property is read by the rule the bean model applies to `@ConditionalOnProperty`: a file of an inactive profile is
 * not loaded, an active profile's file is, and an unresolvable placeholder is reported rather than guessed.
 */
abstract class PageSchemaTestCase : ExplytJavaLightTestCase() {

    protected abstract val stack: WebApplicationStack

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        SOURCES.forEach { (path, text) -> myFixture.addFileToProject(path, text.trimIndent()) }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    protected suspend fun pageSchema(url: String): JsonNode = schemaOf(url, "$PAGE<$ORDER_DTO>")

    protected suspend fun sliceSchema(url: String): JsonNode = schemaOf(url, "$SLICE<$ORDER_DTO>")

    protected suspend fun schemaOf(url: String, declaredReturnType: String): JsonNode {
        assertEquals("the application stack the fixture models", stack, WebApplicationStack.of(module))
        assertResolves(PAGE)
        assertResolves(SLICE)
        val endpoints = mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = project.basePath, httpMethod = "GET")
        )["endpoints"]
        assertEquals("exactly one handler for GET $url: $endpoints", 1, endpoints.size())
        val contract = endpoints.single()
        assertEquals("the handler resolves to the Spring Data type", declaredReturnType, contract["returnType"].asText())
        val schema = contract["responseSchema"]
        assertTrue("a response schema is expected, got $contract", schema != null && !schema.isNull)
        return schema
    }

    protected fun assertDirectPage(schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.domain.PageImpl", schema["className"]?.asText())
        assertEquals("PAGE_DIRECT", schema["wireShape"]?.asText())
        assertEquals(PAGE_DIRECT_KEYS, names(schema).toSet())
        assertEquals(ORDER_DTO, field(schema, "content")["nested"]["className"].asText())
    }

    protected fun assertDirectSlice(schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.domain.SliceImpl", schema["className"]?.asText())
        assertEquals("SLICE_DIRECT", schema["wireShape"]?.asText())
        assertEquals(SLICE_DIRECT_KEYS, names(schema).toSet())
        assertEquals(ORDER_DTO, field(schema, "content")["nested"]["className"].asText())
        assertEquals(
            "a declared Slice may be a PageImpl at run time, so the SliceImpl shape is assumed",
            true, schema["wireShapeAssumed"]?.asBoolean()
        )
    }

    protected fun assertPagedModel(schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.web.PagedModel", schema["className"]?.asText())
        assertEquals("PAGED_MODEL", schema["wireShape"]?.asText())
        assertEquals(setOf("content", "page"), names(schema).toSet())
        assertEquals(ORDER_DTO, field(schema, "content")["nested"]["className"].asText())
        assertEquals(
            setOf("size", "number", "totalElements", "totalPages"),
            names(field(schema, "page")["nested"]).toSet()
        )
    }

    protected fun assertSource(expected: String, schema: JsonNode) {
        assertEquals(schema.toString(), expected, schema["wireShapeSource"]?.asText())
    }

    protected fun assertPageAssumed(schema: JsonNode) {
        assertEquals("a declared Page is taken for PageImpl", true, schema["wireShapeAssumed"]?.asBoolean())
    }

    protected fun addWebConfig(mode: String?) {
        val attribute = mode?.let { "(pageSerializationMode = PageSerializationMode.$it)" }.orEmpty()
        myFixture.addFileToProject(
            "com/example/config/WebConfig.java", """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Configuration
            @EnableSpringDataWebSupport$attribute
            public class WebConfig {
            }
            """.trimIndent()
        )
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        assertResolves(ENABLE_WEB_SUPPORT)
        assertResolves("$ENABLE_WEB_SUPPORT.PageSerializationMode")
    }

    protected fun addFile(path: String, text: String) {
        myFixture.addFileToProject(path, text)
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    protected fun assertResolves(fqn: String) {
        assertNotNull(
            "the fixture must resolve $fqn, or the test proves nothing",
            JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        )
    }

    protected fun keys(schema: JsonNode): List<String> = schema.fieldNames().asSequence().toList()

    protected fun names(schema: JsonNode): List<String> = schema["fields"]?.map { it["name"].asText() }.orEmpty()

    protected fun field(schema: JsonNode, name: String): JsonNode =
        schema["fields"]?.singleOrNull { it["name"].asText() == name }
            ?: throw AssertionError("no field '$name' in $schema")

    protected companion object {
        const val PAGE = "org.springframework.data.domain.Page"
        const val SLICE = "org.springframework.data.domain.Slice"
        const val ENABLE_WEB_SUPPORT = "org.springframework.data.web.config.EnableSpringDataWebSupport"
        const val ORDER_DTO = "com.example.shop.OrderDto"
        const val CUSTOM_PAGE = "com.example.shop.OrderPage"
        const val WEB_CONFIG_SOURCE = "@EnableSpringDataWebSupport on com.example.config.WebConfig"
        const val SERIALIZATION_MODE_KEY = "spring.data.web.pageable.serialization-mode"

        val SLICE_DIRECT_KEYS = setOf(
            "content", "pageable", "first", "last", "size", "number", "sort", "numberOfElements", "empty",
        )
        val PAGE_DIRECT_KEYS = SLICE_DIRECT_KEYS + setOf("totalElements", "totalPages")

        val SOURCES = mapOf(
            "com/example/shop/ShopApplication.java" to """
                package com.example.shop;

                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class ShopApplication {
                }
            """,
            "com/example/shop/OrderDto.java" to """
                package com.example.shop;

                public class OrderDto {
                    private long id;
                    private String note;

                    public long getId() { return id; }
                    public String getNote() { return note; }
                }
            """,
            "com/example/shop/OrderController.java" to """
                package com.example.shop;

                import java.util.List;
                import org.springframework.data.domain.Page;
                import org.springframework.data.domain.Slice;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class OrderController {
                    @GetMapping("/orders/page")
                    public Page<OrderDto> page() { return null; }

                    @GetMapping("/orders/slice")
                    public Slice<OrderDto> slice() { return null; }

                    @GetMapping("/orders/entity-page")
                    public ResponseEntity<Page<OrderDto>> entityPage() { return null; }

                    @GetMapping("/orders/list")
                    public List<OrderDto> list() { return List.of(); }

                    @GetMapping("/orders/one")
                    public OrderDto one() { return new OrderDto(); }
                }
            """,
        )
    }
}

class SpringBootApplicationMcpToolsetPageSchemaTest : PageSchemaTestCase() {

    override val stack = WebApplicationStack.SERVLET

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_4_0,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
        TestLibrary.kotlin_1_9_22,
    )

    fun testPageWithoutWebSupportIsWrittenDirectByDefault() = runBlocking<Unit> {
        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
        assertPageAssumed(schema)
    }

    fun testPageContentIsTheElementSchema() = runBlocking<Unit> {
        val content = field(pageSchema("/orders/page"), "content")

        assertEquals("java.util.List<$ORDER_DTO>", content["type"].asText())
        assertEquals(ORDER_DTO, content["nested"]["className"].asText())
        assertEquals(listOf("id", "note"), names(content["nested"]))
    }

    fun testViaDtoAnnotationWritesPagedModel() = runBlocking<Unit> {
        addWebConfig("VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testViaDtoAnnotationInKotlinWritesPagedModel() = runBlocking<Unit> {
        addFile(
            "com/example/config/KotlinWebConfig.kt", """
            package com.example.config

            import org.springframework.context.annotation.Configuration
            import org.springframework.data.web.config.EnableSpringDataWebSupport
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode

            @Configuration
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
            class KotlinWebConfig
            """.trimIndent()
        )
        assertResolves(ENABLE_WEB_SUPPORT)

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.KotlinWebConfig", schema)
    }

    fun testExplicitDirectAnnotationWritesPageDirect() = runBlocking<Unit> {
        addWebConfig("DIRECT")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testAnnotationWithoutModeAttributeIsDirect() = runBlocking<Unit> {
        addWebConfig(null)

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testViaDtoPropertyWritesPagedModel() = runBlocking<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=via-dto")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testAnnotationOutranksTheProperty() = runBlocking<Unit> {
        addWebConfig("DIRECT")
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=via-dto")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testPlaceholderModeIsReportedUnknownWithTheDefaultShape() = runBlocking<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=\${PAGE_MODE}")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
        assertPageAssumed(schema)
    }

    fun testModeInAnInactiveProfileFileIsNotLoaded() = runBlocking<Unit> {
        addFile("application-prod.properties", "$SERIALIZATION_MODE_KEY=VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
        assertPageAssumed(schema)
    }

    fun testModeInAnActiveProfileFileIsLoaded() = runBlocking<Unit> {
        addFile("application.properties", "spring.profiles.active=prod")
        addFile("application-prod.properties", "$SERIALIZATION_MODE_KEY=VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testSliceIsWrittenDirect() = runBlocking<Unit> {
        assertDirectSlice(sliceSchema("/orders/slice"))
    }

    fun testSliceStaysDirectUnderViaDto() = runBlocking<Unit> {
        addWebConfig("VIA_DTO")

        assertDirectSlice(sliceSchema("/orders/slice"))
    }

    fun testResponseEntityOfPageUnwrapsToThePage() = runBlocking<Unit> {
        val schema = schemaOf("/orders/entity-page", "org.springframework.http.ResponseEntity<$PAGE<$ORDER_DTO>>")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testKotlinSuspendHandlerReturningPageIsThePage() = runBlocking<Unit> {
        addFile(
            "com/example/shop/SuspendOrderController.kt", """
            package com.example.shop

            import org.springframework.data.domain.Page
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class SuspendOrderController {
                @GetMapping("/orders/suspend-page")
                suspend fun page(): Page<OrderDto> = TODO()
            }
            """.trimIndent()
        )

        assertDirectPage(schemaOf("/orders/suspend-page", "$PAGE<$ORDER_DTO>"))
    }

    fun testUnknownModeValueIsReportedUnknownWithTheDefaultShape() = runBlocking<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=garbage")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
    }

    fun testUpperCaseModeValueWritesPagedModel() = runBlocking<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testWebSupportInATestSourceRootDoesNotChangeTheProductionShape() = runBlocking<Unit> {
        val testRoot = myFixture.tempDirFixture.findOrCreateDir("src/test/java")
        PsiTestUtil.addSourceRoot(module, testRoot, true)
        try {
            addFile(
                "src/test/java/com/example/config/TestWebConfig.java", """
                package com.example.config;

                import org.springframework.context.annotation.Configuration;
                import org.springframework.data.web.config.EnableSpringDataWebSupport;
                import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

                @Configuration
                @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
                public class TestWebConfig {
                }
                """.trimIndent()
            )
            assertResolves("com.example.config.TestWebConfig")

            val schema = pageSchema("/orders/page")

            assertDirectPage(schema)
            assertSource("DEFAULT", schema)
        } finally {
            PsiTestUtil.removeSourceRoot(module, testRoot)
        }
    }

    fun testPagesInsideAListAreDescribedWithTheConfiguredShape() = runBlocking<Unit> {
        addWebConfig("VIA_DTO")
        addNestedPagesController()

        val schema = schemaOf("/orders/pages", "java.util.List<$PAGE<$ORDER_DTO>>")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
        assertPageAssumed(schema)
    }

    fun testPageFieldOfADtoIsDescribedWithTheConfiguredShape() = runBlocking<Unit> {
        addWebConfig("VIA_DTO")
        addNestedPagesController()

        val schema = schemaOf("/orders/report", "com.example.shop.OrderReport")

        assertEquals(listOf("orders"), names(schema))
        val orders = field(schema, "orders")
        assertEquals("$PAGE<$ORDER_DTO>", orders["type"].asText())
        assertPagedModel(orders["nested"])
        assertSource(WEB_CONFIG_SOURCE, orders["nested"])
    }

    fun testPageOfAnUnresolvedElementKeepsThePageShape() = runBlocking<Unit> {
        addFile(
            "com/example/shop/UnresolvedController.java", """
            package com.example.shop;

            import org.springframework.data.domain.Page;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class UnresolvedController {
                @GetMapping("/orders/missing")
                public Page<Missing> missing() { return null; }
            }
            """.trimIndent()
        )
        assertNull(
            "the element type must stay unresolved",
            JavaPsiFacade.getInstance(project).findClass("com.example.shop.Missing", GlobalSearchScope.allScope(project))
        )

        val schema = schemaOf("/orders/missing", "$PAGE<Missing>")

        assertEquals(schema.toString(), "org.springframework.data.domain.PageImpl", schema["className"].asText())
        assertEquals("PAGE_DIRECT", schema["wireShape"].asText())
        assertEquals(PAGE_DIRECT_KEYS, names(schema).toSet())
        val content = field(schema, "content")
        assertEquals("java.util.List<Missing>", content["type"].asText())
        assertTrue("an unresolved element has no schema: $content", content["nested"].isNull)
    }

    fun testCustomPageImplSubclassIsDescribedByItsGetters() = runBlocking<Unit> {
        addCustomPage()

        val schema = schemaOf("/orders/custom-page", "$CUSTOM_PAGE<$ORDER_DTO>")

        assertEquals(CUSTOM_PAGE, schema["className"].asText())
        assertFalse("a project page is not one of the Spring Data classes: $schema", schema.has("wireShape"))
        assertTrue(
            "the page properties inherited from PageImpl are written: ${names(schema)}",
            names(schema).containsAll(PAGE_DIRECT_KEYS - "empty")
        )
    }

    fun testOwnPageImplementationIsNotRewrappedUnderViaDto() = runBlocking<Unit> {
        addWebConfig("VIA_DTO")
        addFile(
            "com/example/shop/WindowPage.java", """
            package com.example.shop;

            import org.springframework.data.domain.Page;

            public abstract class WindowPage<T> implements Page<T> {
                public String getWindow() { return ""; }
            }
            """.trimIndent()
        )
        addFile(
            "com/example/shop/WindowPageController.java", """
            package com.example.shop;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class WindowPageController {
                @GetMapping("/orders/window-page")
                public WindowPage<OrderDto> page() { return null; }
            }
            """.trimIndent()
        )
        assertResolves("com.example.shop.WindowPage")

        val schema = schemaOf("/orders/window-page", "com.example.shop.WindowPage<$ORDER_DTO>")

        assertEquals(schema.toString(), "com.example.shop.WindowPage", schema["className"].asText())
        assertFalse("a Page not extending PageImpl is written by itself: $schema", "page" in names(schema))
        assertFalse(schema.toString(), schema["wireShape"]?.asText() == "PAGED_MODEL")
        assertTrue("its own getter is written: $schema", "window" in names(schema))
    }

    fun testSliceImplSubclassStaysDirectUnderViaDto() = runBlocking<Unit> {
        addWebConfig("VIA_DTO")
        addFile(
            "com/example/shop/OrderSlice.java", """
            package com.example.shop;

            import java.util.List;
            import org.springframework.data.domain.SliceImpl;

            public class OrderSlice<T> extends SliceImpl<T> {
                public OrderSlice(List<T> content) { super(content); }
            }
            """.trimIndent()
        )
        addFile(
            "com/example/shop/OrderSliceController.java", """
            package com.example.shop;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OrderSliceController {
                @GetMapping("/orders/custom-slice")
                public OrderSlice<OrderDto> slice() { return null; }
            }
            """.trimIndent()
        )
        assertResolves("org.springframework.data.domain.SliceImpl")

        val schema = schemaOf("/orders/custom-slice", "com.example.shop.OrderSlice<$ORDER_DTO>")

        assertFalse("a slice is never re-wrapped: $schema", "page" in names(schema))
        assertFalse(schema.toString(), schema["wireShape"]?.asText() == "PAGED_MODEL")
        assertEquals(schema.toString(), SLICE_DIRECT_KEYS, names(schema).toSet())
    }

    private fun addCustomPage() {
        addFile(
            "com/example/shop/OrderPage.java", """
            package com.example.shop;

            import java.util.List;
            import org.springframework.data.domain.PageImpl;

            public class OrderPage<T> extends PageImpl<T> {
                public OrderPage(List<T> content) { super(content); }
            }
            """.trimIndent()
        )
        addFile(
            "com/example/shop/CustomPageController.java", """
            package com.example.shop;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class CustomPageController {
                @GetMapping("/orders/custom-page")
                public OrderPage<OrderDto> page() { return null; }
            }
            """.trimIndent()
        )
        assertResolves("org.springframework.data.domain.PageImpl")
    }

    private fun addNestedPagesController() {
        addFile(
            "com/example/shop/OrderReport.java", """
            package com.example.shop;

            import org.springframework.data.domain.Page;

            public class OrderReport {
                public Page<OrderDto> getOrders() { return null; }
            }
            """.trimIndent()
        )
        addFile(
            "com/example/shop/NestedPagesController.java", """
            package com.example.shop;

            import java.util.List;
            import org.springframework.data.domain.Page;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class NestedPagesController {
                @GetMapping("/orders/pages")
                public List<Page<OrderDto>> pages() { return List.of(); }

                @GetMapping("/orders/report")
                public OrderReport report() { return new OrderReport(); }
            }
            """.trimIndent()
        )
    }

    fun testListOfDtoIsUnchanged() = runBlocking<Unit> {
        val schema = schemaOf("/orders/list", "java.util.List<$ORDER_DTO>")

        assertEquals(listOf("className", "fields"), keys(schema))
        assertEquals(ORDER_DTO, schema["className"].asText())
        assertEquals(listOf("id", "note"), names(schema))
    }

    fun testPlainDtoIsUnchanged() = runBlocking<Unit> {
        val schema = schemaOf("/orders/one", ORDER_DTO)

        assertEquals(listOf("className", "fields"), keys(schema))
        assertEquals(listOf("id", "note"), names(schema))
    }
}

/**
 * Boot's `SpringDataWebAutoConfiguration`, which binds `spring.data.web.pageable.serialization-mode`, is servlet-only,
 * so the property means nothing to a reactive application. `@EnableSpringDataWebSupport` itself is stack-neutral: its
 * import selector adds `SpringDataJacksonConfiguration`, whose `PageModule` bean Boot installs into the `ObjectMapper`
 * the WebFlux codecs use.
 */
class SpringBootApplicationMcpToolsetReactivePageSchemaTest : PageSchemaTestCase() {

    override val stack = WebApplicationStack.REACTIVE

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springReactiveWeb_3_1_1,
        TestLibrary.springDataJpa_3_4_0,
    )

    fun testPropertyIsIgnoredOnAReactiveApplication() = runBlocking<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=via-dto")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
        assertPageAssumed(schema)
    }

    fun testWebSupportAnnotationOnAReactiveApplicationIsUnknown() = runBlocking<Unit> {
        assertNull(
            "reactive fixture must not resolve Spring MVC WebConfigurer",
            JavaPsiFacade.getInstance(project).findClass(
                "org.springframework.web.servlet.config.annotation.WebMvcConfigurer",
                GlobalSearchScope.allScope(project),
            )
        )
        addWebConfig("VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
        assertPageAssumed(schema)
    }

    fun testPlainDtoIsUnchangedOnAReactiveApplication() = runBlocking<Unit> {
        val schema = schemaOf("/orders/one", ORDER_DTO)

        assertEquals(listOf("className", "fields"), keys(schema))
        assertEquals(listOf("id", "note"), names(schema))
    }
}
