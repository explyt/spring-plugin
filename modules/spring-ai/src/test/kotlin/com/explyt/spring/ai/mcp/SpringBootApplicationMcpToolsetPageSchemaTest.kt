/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.TestCompiledLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.explyt.spring.web.util.WebApplicationStack
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil

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
        assertEquals(
            "the handler resolves to the Spring Data type",
            declaredReturnType,
            contract["returnType"].asText()
        )
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
        const val PAGED_MODEL = "org.springframework.data.web.PagedModel"
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
                import org.springframework.data.web.PagedModel;
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

                @GetMapping("/orders/model")
                    public PagedModel<OrderDto> pagedModel() { return null; }

                @GetMapping("/orders/entity-model")
                    public ResponseEntity<PagedModel<OrderDto>> entityPagedModel() { return null; }

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

    fun testPageWithoutWebSupportIsWrittenDirectByDefault() = runBlockingReleasingWriteIntent<Unit> {
        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
        assertPageAssumed(schema)
    }

    fun testPageContentIsTheElementSchema() = runBlockingReleasingWriteIntent<Unit> {
        val content = field(pageSchema("/orders/page"), "content")

        assertEquals("java.util.List<$ORDER_DTO>", content["type"].asText())
        assertEquals(ORDER_DTO, content["nested"]["className"].asText())
        assertEquals(listOf("id", "note"), names(content["nested"]))
    }

    fun testViaDtoAnnotationWritesPagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testViaDtoAnnotationInKotlinWritesPagedModel() = runBlockingReleasingWriteIntent<Unit> {
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

    fun testExplicitDirectAnnotationWritesPageDirect() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("DIRECT")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testAnnotationWithoutModeAttributeIsDirect() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig(null)

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testViaDtoPropertyWritesPagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=via-dto")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testAnnotationOutranksTheProperty() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("DIRECT")
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=via-dto")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testPlaceholderModeIsReportedUnknownWithTheDefaultShape() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=\${PAGE_MODE}")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
        assertPageAssumed(schema)
    }

    fun testModeInAnInactiveProfileFileIsNotLoaded() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application-prod.properties", "$SERIALIZATION_MODE_KEY=VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
        assertPageAssumed(schema)
    }

    fun testModeInAnActiveProfileFileIsLoaded() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "spring.profiles.active=prod")
        addFile("application-prod.properties", "$SERIALIZATION_MODE_KEY=VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testSliceIsWrittenDirect() = runBlockingReleasingWriteIntent<Unit> {
        assertDirectSlice(sliceSchema("/orders/slice"))
    }

    fun testSliceStaysDirectUnderViaDto() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("VIA_DTO")

        assertDirectSlice(sliceSchema("/orders/slice"))
    }

    fun testResponseEntityOfPageUnwrapsToThePage() = runBlockingReleasingWriteIntent<Unit> {
        val schema = schemaOf("/orders/entity-page", "org.springframework.http.ResponseEntity<$PAGE<$ORDER_DTO>>")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testPagedModelReturnDescribesItsDeclaredWireShape() = runBlockingReleasingWriteIntent<Unit> {
        val schema = schemaOf("/orders/model", "$PAGED_MODEL<$ORDER_DTO>")

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testPagedModelReturnKeepsItsDeclaredWireShapeWithViaDtoProperty() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=via-dto")

        val schema = schemaOf("/orders/model", "$PAGED_MODEL<$ORDER_DTO>")

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testResponseEntityOfPagedModelDescribesItsDeclaredWireShape() = runBlockingReleasingWriteIntent<Unit> {
        val schema = schemaOf(
            "/orders/entity-model",
            "org.springframework.http.ResponseEntity<$PAGED_MODEL<$ORDER_DTO>>",
        )

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testKotlinSuspendHandlerReturningPagedModelIsThePagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addFile(
            "com/example/shop/SuspendPagedModelController.kt", """
            package com.example.shop

            import org.springframework.data.web.PagedModel
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            class SuspendPagedModelController {
                @GetMapping("/orders/suspend-model")
                suspend fun page(): PagedModel<OrderDto> = TODO()
            }
            """.trimIndent()
        )

        val schema = schemaOf("/orders/suspend-model", "$PAGED_MODEL<$ORDER_DTO>")

        assertPagedModel(schema)
        assertSource("DECLARED_TYPE", schema)
    }

    fun testBlankSerializationModeUsesBootDirectDefault() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
    }

    fun testComposedWebSupportAnnotationControlsPageSerialization() = runBlockingReleasingWriteIntent<Unit> {
        addFile(
            "com/example/config/EnableMyWeb.java", """
            package com.example.config;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
            public @interface EnableMyWeb {
            }
            """.trimIndent()
        )
        addFile(
            "com/example/config/ComposedWebConfig.java", """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;

            @Configuration
            @EnableMyWeb
            public class ComposedWebConfig {
            }
            """.trimIndent()
        )

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on com.example.config.ComposedWebConfig", schema)
    }

    fun testKotlinSuspendHandlerReturningPageIsThePage() = runBlockingReleasingWriteIntent<Unit> {
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

    fun testUnknownModeValueIsReportedUnknownWithTheDefaultShape() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=garbage")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
    }

    fun testUpperCaseModeValueWritesPagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testWebSupportInATestSourceRootDoesNotChangeTheProductionShape() = runBlockingReleasingWriteIntent<Unit> {
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

    fun testPagesInsideAListAreDescribedWithTheConfiguredShape() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("VIA_DTO")
        addNestedPagesController()

        val schema = schemaOf("/orders/pages", "java.util.List<$PAGE<$ORDER_DTO>>")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
        assertPageAssumed(schema)
    }

    fun testPageFieldOfADtoIsDescribedWithTheConfiguredShape() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("VIA_DTO")
        addNestedPagesController()

        val schema = schemaOf("/orders/report", "com.example.shop.OrderReport")

        assertEquals(listOf("orders"), names(schema))
        val orders = field(schema, "orders")
        assertEquals("$PAGE<$ORDER_DTO>", orders["type"].asText())
        assertPagedModel(orders["nested"])
        assertSource(WEB_CONFIG_SOURCE, orders["nested"])
    }

    fun testPageOfAnUnresolvedElementKeepsThePageShape() = runBlockingReleasingWriteIntent<Unit> {
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
            JavaPsiFacade.getInstance(project)
                .findClass("com.example.shop.Missing", GlobalSearchScope.allScope(project))
        )

        val schema = schemaOf("/orders/missing", "$PAGE<Missing>")

        assertEquals(schema.toString(), "org.springframework.data.domain.PageImpl", schema["className"].asText())
        assertEquals("PAGE_DIRECT", schema["wireShape"].asText())
        assertEquals(PAGE_DIRECT_KEYS, names(schema).toSet())
        val content = field(schema, "content")
        assertEquals("java.util.List<Missing>", content["type"].asText())
        assertTrue("an unresolved element has no schema: $content", content["nested"].isNull)
    }

    fun testDirectPageImplSubclassIsWrittenWithEveryPageKeyAndItsOwnGetters() = runBlockingReleasingWriteIntent<Unit> {
        addCustomPage()

        val schema = schemaOf("/orders/custom-page", "$CUSTOM_PAGE<$ORDER_DTO>")

        assertEquals(schema.toString(), PAGE_DIRECT_KEYS + "tag", names(schema).toSet())
        assertEquals(schema.toString(), ORDER_DTO, field(schema, "content")["nested"]?.get("className")?.asText())
        assertPageableAndSortAsWritten(schema)
    }

    fun testUnknownModeLeavesAPageImplSubclassDirectAndSaysSo() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=\${PAGE_MODE}")
        addCustomPage()

        val schema = schemaOf("/orders/custom-page", "$CUSTOM_PAGE<$ORDER_DTO>")

        assertEquals(schema.toString(), "PAGE_DIRECT", schema["wireShape"]?.asText())
        assertSource("UNKNOWN", schema)
    }

    fun testPageImplSubclassWithAFixedElementIsRewrappedUnderViaDto() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("VIA_DTO")
        addFile(
            "com/example/shop/OrderDtoPage.java", """
            package com.example.shop;

            import java.util.List;
            import org.springframework.data.domain.PageImpl;

            public class OrderDtoPage extends PageImpl<OrderDto> {
                public OrderDtoPage(List<OrderDto> content) { super(content); }
            }
            """.trimIndent()
        )
        addFile(
            "com/example/shop/OrderDtoPageController.java", """
            package com.example.shop;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class OrderDtoPageController {
                @GetMapping("/orders/fixed-page")
                public OrderDtoPage page() { return null; }
            }
            """.trimIndent()
        )
        assertResolves("com.example.shop.OrderDtoPage")

        val schema = schemaOf("/orders/fixed-page", "com.example.shop.OrderDtoPage")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testPageImplSubclassIsRewrappedUnderViaDto() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("VIA_DTO")
        addCustomPage()

        val schema = schemaOf("/orders/custom-page", "$CUSTOM_PAGE<$ORDER_DTO>")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testOwnPageImplementationIsNotRewrappedUnderViaDto() = runBlockingReleasingWriteIntent<Unit> {
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

    fun testSliceImplSubclassStaysDirectUnderViaDto() = runBlockingReleasingWriteIntent<Unit> {
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

                public String getTag() { return ""; }
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

    fun testDirectPageDescribesPageableAndSortAsWritten() = runBlockingReleasingWriteIntent<Unit> {
        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertPageableAndSortAsWritten(schema)
    }

    fun testDirectSliceDescribesPageableAndSortAsWritten() = runBlockingReleasingWriteIntent<Unit> {
        val schema = sliceSchema("/orders/slice")

        assertDirectSlice(schema)
        assertPageableAndSortAsWritten(schema)
    }

    fun testViaDtoDeclarationWinsOverAnEarlierDirectOne() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfigNamed("AlphaWebConfig", "DIRECT")
        addWebConfig("VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testTwoDirectDeclarationsStayDirect() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfigNamed("AlphaWebConfig", "DIRECT")
        addWebConfig("DIRECT")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertTrue(schema.toString(), schema["wireShapeSource"].asText().startsWith("@EnableSpringDataWebSupport on "))
    }

    fun testTwoViaDtoDeclarationsFailStartupSoTheShapeIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfigNamed("AlphaWebConfig", "VIA_DTO")
        addWebConfig("VIA_DTO")
        assertDeclaresWebSupport(ALPHA_WEB_CONFIG, WEB_CONFIG)

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
        assertPageAssumed(schema)
    }

    fun testTwoKotlinViaDtoDeclarationsFailStartupSoTheShapeIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addKotlinWebConfigNamed("AlphaKotlinWebConfig", "VIA_DTO")
        addKotlinWebConfigNamed("BetaKotlinWebConfig", "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
        assertPageAssumed(schema)
    }

    fun testTwoViaDtoDeclarationsWithOverridingExplicitlyDisabledAreUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$ALLOW_BEAN_DEFINITION_OVERRIDING_KEY=false")
        addWebConfigNamed("AlphaWebConfig", "VIA_DTO")
        addWebConfig("VIA_DTO")
        assertDeclaresWebSupport(ALPHA_WEB_CONFIG, WEB_CONFIG)

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testTwoViaDtoDeclarationsWithBeanOverridingWritePagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$ALLOW_BEAN_DEFINITION_OVERRIDING_KEY=true")
        addWebConfigNamed("AlphaWebConfig", "VIA_DTO")
        addWebConfig("VIA_DTO")
        assertDeclaresWebSupport(ALPHA_WEB_CONFIG, WEB_CONFIG)

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSourceIsOneOf(schema, ALPHA_WEB_CONFIG, WEB_CONFIG)
    }

    fun testTwoKotlinViaDtoDeclarationsWithBeanOverridingWritePagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$ALLOW_BEAN_DEFINITION_OVERRIDING_KEY=true")
        addKotlinWebConfigNamed("AlphaKotlinWebConfig", "VIA_DTO")
        addKotlinWebConfigNamed("BetaKotlinWebConfig", "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSourceIsOneOf(schema, "$CONFIG_PACKAGE.AlphaKotlinWebConfig", "$CONFIG_PACKAGE.BetaKotlinWebConfig")
    }

    fun testKotlinViaDtoDeclarationWinsOverAKotlinDirectOne() = runBlockingReleasingWriteIntent<Unit> {
        addKotlinWebConfigNamed("AlphaKotlinWebConfig", "DIRECT")
        addKotlinWebConfigNamed("BetaKotlinWebConfig", "VIA_DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource("@EnableSpringDataWebSupport on $CONFIG_PACKAGE.BetaKotlinWebConfig", schema)
    }

    fun testViaDtoBesideAnUnreadableModeIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addWebConfig("VIA_DTO")
        addUnreadableModeWebConfig()
        assertDeclaresWebSupport(WEB_CONFIG)

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
        assertPageAssumed(schema)
    }

    fun testViaDtoBesideAnUnreadableModeWithBeanOverridingWritesPagedModel() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$ALLOW_BEAN_DEFINITION_OVERRIDING_KEY=true")
        addWebConfig("VIA_DTO")
        addUnreadableModeWebConfig()
        assertDeclaresWebSupport(WEB_CONFIG)

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testTwoViaDtoDeclarationsWithUnresolvableOverridingAreUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$ALLOW_BEAN_DEFINITION_OVERRIDING_KEY=\${UNSET_OVERRIDING}")
        addWebConfigNamed("AlphaWebConfig", "VIA_DTO")
        addWebConfig("VIA_DTO")
        assertDeclaresWebSupport(ALPHA_WEB_CONFIG, WEB_CONFIG)

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
    }

    fun testOneViaDtoDeclarationIgnoresAnUnresolvableOverridingValue() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$ALLOW_BEAN_DEFINITION_OVERRIDING_KEY=\${UNSET_OVERRIDING}")
        addWebConfig("VIA_DTO")
        assertDeclaresWebSupport(WEB_CONFIG)

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(WEB_CONFIG_SOURCE, schema)
    }

    fun testUnreadableModeAloneIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addUnreadableModeWebConfig()

        val schema = pageSchema("/orders/page")

        assertSource("UNKNOWN", schema)
        assertDirectPage(schema)
        assertPageAssumed(schema)
    }

    private fun addUnreadableModeWebConfig() {
        addFile(
            "com/example/config/EnableComposedWeb.java", """
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
            @EnableSpringDataWebSupport
            public @interface EnableComposedWeb {
                @AliasFor(annotation = EnableSpringDataWebSupport.class, attribute = "pageSerializationMode")
                PageSerializationMode mode() default PageSerializationMode.VIA_DTO;
            }
            """.trimIndent()
        )
        addFile(
            "com/example/config/ComposedWebConfig.java", """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;

            @Configuration
            @EnableComposedWeb
            public class ComposedWebConfig {
            }
            """.trimIndent()
        )
        assertUsesAnAliasedModeDefault(COMPOSED_WEB_CONFIG)
    }

    private fun assertUsesAnAliasedModeDefault(fqn: String) {
        val facade = JavaPsiFacade.getInstance(project)
        val composed = facade.findClass(COMPOSED_WEB_SUPPORT, GlobalSearchScope.allScope(project))
        assertNotNull("the fixture must resolve $COMPOSED_WEB_SUPPORT", composed)
        assertTrue("$COMPOSED_WEB_SUPPORT is an annotation type", composed!!.isAnnotationType)
        assertTrue("$COMPOSED_WEB_SUPPORT is meta-annotated", composed.hasAnnotation(ENABLE_WEB_SUPPORT))
        val mode = composed.findMethodsByName("mode", false).singleOrNull()
        assertNotNull("$COMPOSED_WEB_SUPPORT declares 'mode'", mode)
        assertTrue("'mode' is an @AliasFor of pageSerializationMode", mode!!.hasAnnotation(ALIAS_FOR))
        val using = facade.findClass(fqn, GlobalSearchScope.projectScope(project))
        assertNotNull("the fixture must declare $fqn, or the test proves nothing", using)
        val use = using!!.getAnnotation(COMPOSED_WEB_SUPPORT)
        assertNotNull("$fqn uses $COMPOSED_WEB_SUPPORT", use)
        assertEmpty("$fqn leaves the mode to the composed default", use!!.parameterList.attributes.toList())
    }

    private fun addKotlinWebConfigNamed(className: String, mode: String) {
        addFile(
            "com/example/config/$className.kt", """
            package com.example.config

            import org.springframework.context.annotation.Configuration
            import org.springframework.data.web.config.EnableSpringDataWebSupport
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode

            @Configuration
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.$mode)
            class $className
            """.trimIndent()
        )
        assertDeclaresWebSupport("$CONFIG_PACKAGE.$className")
    }

    private fun assertDeclaresWebSupport(vararg fqns: String) {
        fqns.forEach { fqn ->
            val declaration = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.projectScope(project))
            assertNotNull("the fixture must declare $fqn, or the test proves nothing", declaration)
            assertTrue("$fqn must carry @EnableSpringDataWebSupport", declaration!!.hasAnnotation(ENABLE_WEB_SUPPORT))
        }
    }

    private fun assertSourceIsOneOf(schema: JsonNode, vararg declarations: String) {
        val sources = declarations.map { "@EnableSpringDataWebSupport on $it" }
        assertTrue("$schema names one of $sources", schema["wireShapeSource"]?.asText() in sources)
    }

    fun testCamelCaseModeValueIsReadLeniently() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=viaDto")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testDottedModeValueIsReadLeniently() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=VIA.DTO")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertSource(SERIALIZATION_MODE_KEY, schema)
    }

    fun testClassNameAsModeValueIsReportedUnknown() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=PagedModel")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("UNKNOWN", schema)
    }

    fun testLibraryViaDtoConfigurationImportedByTheApplicationWritesPagedModel() {
        attachVendorWebSupportLibrary()
        runBlockingReleasingWriteIntent<Unit> {
            addFile("com/example/shop/VendorImports.java", VENDOR_IMPORTS_SOURCE)
            assertLibraryDeclaresViaDto(VENDOR_WEB_CONFIG)
            assertTrue(
                "precondition: the bean model lists the imported $VENDOR_WEB_CONFIG as an active bean",
                isActiveBean(VENDOR_WEB_CONFIG)
            )

            val schema = pageSchema("/orders/page")

            assertPagedModel(schema)
            assertSource("@EnableSpringDataWebSupport on $VENDOR_WEB_CONFIG", schema)
        }
    }

    fun testLibraryViaDtoConfigurationNeitherImportedNorScannedLeavesThePageDirect() {
        attachVendorWebSupportLibrary()
        runBlockingReleasingWriteIntent<Unit> {
            assertLibraryDeclaresViaDto(VENDOR_WEB_CONFIG)
            val vendorConfig = JavaPsiFacade.getInstance(project)
                .findClass(VENDOR_WEB_CONFIG, GlobalSearchScope.allScope(project))!!
            assertNull(
                "precondition: no project class imports $VENDOR_WEB_CONFIG",
                ReferencesSearch.search(vendorConfig, GlobalSearchScope.projectScope(project)).findFirst()
            )

            val schema = pageSchema("/orders/page")

            assertDirectPage(schema)
            assertSource("DEFAULT", schema)
        }
    }

    fun testUnreachedLibraryViaDtoAutoConfigurationLeavesTheShapeUnknown() {
        attachVendorAutoConfiguration("VIA_DTO")
        runBlockingReleasingWriteIntent<Unit> {
            assertUnreachedAutoConfigurationCandidate(VENDOR_AUTO_CONFIGURATION, "VIA_DTO", annotated = true)

            val schema = pageSchema("/orders/page")

            assertSource("UNKNOWN", schema)
            assertDirectPage(schema)
            assertPageAssumed(schema)
        }
    }

    fun testUnreachedLibraryViaDtoConfigurationListedInAutoConfigurationImportsLeavesTheShapeUnknown() {
        TestCompiledLibrary.attach(
            module, testRootDisposable, "vendor-listed-web-support",
            mapOf("com/vendor/web/VendorWebConfig.java" to VENDOR_WEB_CONFIG_SOURCE),
            mapOf(AUTO_CONFIGURATION_IMPORTS_PATH to "$VENDOR_WEB_CONFIG\n"),
        )
        runBlockingReleasingWriteIntent<Unit> {
            assertUnreachedAutoConfigurationCandidate(VENDOR_WEB_CONFIG, "VIA_DTO", annotated = false)

            val schema = pageSchema("/orders/page")

            assertSource("UNKNOWN", schema)
            assertDirectPage(schema)
            assertPageAssumed(schema)
        }
    }

    fun testUnreachedLibraryViaDtoAutoConfigurationBesideAProjectViaDtoLeavesTheShapeUnknown() {
        attachVendorAutoConfiguration("VIA_DTO")
        runBlockingReleasingWriteIntent<Unit> {
            addWebConfig("VIA_DTO")
            assertDeclaresWebSupport(WEB_CONFIG)
            assertUnreachedAutoConfigurationCandidate(VENDOR_AUTO_CONFIGURATION, "VIA_DTO", annotated = true)

            val schema = pageSchema("/orders/page")

            assertSource("UNKNOWN", schema)
            assertDirectPage(schema)
            assertPageAssumed(schema)
        }
    }

    fun testUnreachedLibraryViaDtoAutoConfigurationBesideAProjectViaDtoWithBeanOverridingWritesPagedModel() {
        attachVendorAutoConfiguration("VIA_DTO")
        runBlockingReleasingWriteIntent<Unit> {
            addFile("application.properties", "$ALLOW_BEAN_DEFINITION_OVERRIDING_KEY=true")
            addWebConfig("VIA_DTO")
            assertDeclaresWebSupport(WEB_CONFIG)
            assertUnreachedAutoConfigurationCandidate(VENDOR_AUTO_CONFIGURATION, "VIA_DTO", annotated = true)

            val schema = pageSchema("/orders/page")

            assertPagedModel(schema)
            assertSource(WEB_CONFIG_SOURCE, schema)
        }
    }

    fun testUnreachedLibraryDirectAutoConfigurationLeavesThePageDirect() {
        attachVendorAutoConfiguration("DIRECT")
        runBlockingReleasingWriteIntent<Unit> {
            assertUnreachedAutoConfigurationCandidate(VENDOR_AUTO_CONFIGURATION, "DIRECT", annotated = true)

            val schema = pageSchema("/orders/page")

            assertDirectPage(schema)
            assertSource("DEFAULT", schema)
        }
    }

    fun testBootDataWebAutoConfigurationUnderADefaultPackageScanLeavesTheModeToTheProperty() =
        runBlockingReleasingWriteIntent<Unit> {
            addFile(
                "DefaultPackageApplication.java", """
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class DefaultPackageApplication {
                }
                """.trimIndent()
            )
            addFile("application.properties", "$SERIALIZATION_MODE_KEY=via_dto")
            assertResolves("DefaultPackageApplication")
            val bootConfig = JavaPsiFacade.getInstance(project)
                .findClass(BOOT_DATA_WEB_AUTO_CONFIGURATION, GlobalSearchScope.allScope(project))
            assertNotNull("the fixture must resolve $BOOT_DATA_WEB_AUTO_CONFIGURATION", bootConfig)
            val file = bootConfig!!.containingFile.virtualFile
            assertTrue(
                "$BOOT_DATA_WEB_AUTO_CONFIGURATION must come from library classes",
                ProjectFileIndex.getInstance(project).isInLibraryClasses(file)
            )
            assertFalse("it must be outside the project scope", ProjectScope.getProjectScope(project).contains(file))
            assertTrue("it carries @EnableSpringDataWebSupport", bootConfig.hasAnnotation(ENABLE_WEB_SUPPORT))
            assertEmpty(
                "it declares no mode, so the annotation alone would mean DIRECT",
                bootConfig.getAnnotation(ENABLE_WEB_SUPPORT)!!.parameterList.attributes.toList()
            )
            assertTrue(
                "it is an @AutoConfiguration",
                bootConfig.hasAnnotation(SpringCoreClasses.BOOT_AUTO_CONFIGURATION)
            )
            val scanned = PackageScanService.getInstance(project).getAllPackages().getPackages(module)
            assertTrue("precondition: the default-package application scans every package: $scanned", "" in scanned)
            assertTrue(
                "precondition: the scan covers $BOOT_DATA_WEB_AUTO_CONFIGURATION: $scanned",
                scanned.any(BOOT_DATA_WEB_AUTO_CONFIGURATION::startsWith)
            )

            val schema = pageSchema("/orders/page")

            assertPagedModel(schema)
            assertSource(SERIALIZATION_MODE_KEY, schema)
        }

    fun testLibraryViaDtoAutoConfigurationImportedByTheApplicationWritesPagedModel() {
        attachVendorAutoConfiguration("VIA_DTO")
        runBlockingReleasingWriteIntent<Unit> {
            addFile(
                "com/example/shop/VendorAutoConfigurationImports.java", """
                package com.example.shop;

                import com.vendor.web.VendorWebAutoConfiguration;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.context.annotation.Import;

                @Configuration
                @Import(VendorWebAutoConfiguration.class)
                public class VendorAutoConfigurationImports {
                }
                """.trimIndent()
            )
            val declaration = JavaPsiFacade.getInstance(project)
                .findClass(VENDOR_AUTO_CONFIGURATION, GlobalSearchScope.allScope(project))
            assertNotNull("the fixture must resolve $VENDOR_AUTO_CONFIGURATION", declaration)
            val file = declaration!!.containingFile.virtualFile
            assertTrue(
                "$VENDOR_AUTO_CONFIGURATION must come from compiled library classes",
                ProjectFileIndex.getInstance(project).isInLibraryClasses(file)
            )
            assertFalse("it must be outside the project scope", ProjectScope.getProjectScope(project).contains(file))
            assertTrue(
                "it is an @AutoConfiguration",
                declaration.isMetaAnnotatedBy(SpringCoreClasses.BOOT_AUTO_CONFIGURATION)
            )
            assertEquals(
                "precondition: the application's own configuration is the only importer",
                setOf("com.example.shop.VendorAutoConfigurationImports"),
                PackageScanService.getInstance(project).getAllPackages().importersByImported[VENDOR_AUTO_CONFIGURATION]
            )

            val schema = pageSchema("/orders/page")

            assertPagedModel(schema)
            assertSource("@EnableSpringDataWebSupport on $VENDOR_AUTO_CONFIGURATION", schema)
        }
    }

    private fun attachVendorAutoConfiguration(mode: String) {
        TestCompiledLibrary.attach(
            module, testRootDisposable, "vendor-web-auto-configuration",
            mapOf("com/vendor/web/VendorWebAutoConfiguration.java" to vendorAutoConfigurationSource(mode))
        )
    }

    private fun assertUnreachedAutoConfigurationCandidate(fqn: String, mode: String, annotated: Boolean) {
        val declaration = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        assertNotNull("the fixture must resolve the library class $fqn, or the test proves nothing", declaration)
        val file = declaration!!.containingFile.virtualFile
        assertTrue(
            "$fqn must come from compiled library classes",
            ProjectFileIndex.getInstance(project).isInLibraryClasses(file)
        )
        assertFalse("$fqn must be outside the project scope", ProjectScope.getProjectScope(project).contains(file))
        val declaredMode = declaration.getAnnotation(ENABLE_WEB_SUPPORT)
            ?.findDeclaredAttributeValue("pageSerializationMode")?.text.orEmpty()
        assertTrue("$fqn must declare $mode, got '$declaredMode'", declaredMode.endsWith(mode))

        assertNull(
            "precondition: no project class references $fqn",
            ReferencesSearch.search(declaration, GlobalSearchScope.projectScope(project)).findFirst()
        )
        val scan = PackageScanService.getInstance(project).getAllPackages()
        assertFalse("precondition: no @Import names $fqn", fqn in scan.importQualified)
        val scanned = scan.getPackages(module)
        assertTrue("precondition: the project scans packages, got $scanned", scanned.isNotEmpty())
        assertTrue("precondition: no scanned package covers $fqn: $scanned", scanned.none(fqn::startsWith))

        assertEquals(
            "precondition: $fqn is meta-annotated with @AutoConfiguration",
            annotated, declaration.isMetaAnnotatedBy(SpringCoreClasses.BOOT_AUTO_CONFIGURATION)
        )
        assertEquals(
            "precondition: $fqn is listed in $AUTO_CONFIGURATION_IMPORTS_PATH",
            !annotated, fqn in SpringConfigurationPropertiesSearch.getInstance(project).getAllFactoriesNames(module)
        )
    }

    private fun attachVendorWebSupportLibrary() {
        TestCompiledLibrary.attach(
            module, testRootDisposable, "vendor-web-support",
            mapOf("com/vendor/web/VendorWebConfig.java" to VENDOR_WEB_CONFIG_SOURCE)
        )
    }

    private fun assertLibraryDeclaresViaDto(fqn: String) {
        val declaration = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        assertNotNull("the fixture must resolve the library class $fqn, or the test proves nothing", declaration)
        val file = declaration!!.containingFile.virtualFile
        assertTrue(
            "$fqn must come from compiled library classes",
            ProjectFileIndex.getInstance(project).isInLibraryClasses(file)
        )
        assertFalse("$fqn must be outside the project scope", ProjectScope.getProjectScope(project).contains(file))
        val webSupport = declaration.getAnnotation(ENABLE_WEB_SUPPORT)
        assertNotNull("$fqn must carry @EnableSpringDataWebSupport", webSupport)
        val mode = webSupport!!.findDeclaredAttributeValue("pageSerializationMode")?.text.orEmpty()
        assertTrue("$fqn must declare VIA_DTO, got '$mode'", mode.endsWith("VIA_DTO"))
    }

    private fun isActiveBean(fqn: String): Boolean =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .any { it.psiClass.qualifiedName == fqn }

    private fun assertPageableAndSortAsWritten(schema: JsonNode) {
        val sort = field(schema, "sort")["nested"]
        assertNotNull("sort carries its written properties: $schema", sort?.takeUnless { it.isNull })
        assertNull(sort.toString(), sort["schemaOmitted"])
        assertEquals(
            sort.toString(),
            mapOf("empty" to "boolean", "sorted" to "boolean", "unsorted" to "boolean"),
            typesOf(sort)
        )
        val pageable = field(schema, "pageable")["nested"]
        assertNotNull("pageable carries its written properties: $schema", pageable?.takeUnless { it.isNull })
        assertNull(pageable.toString(), pageable["schemaOmitted"])
        assertEquals(
            pageable.toString(),
            mapOf(
                "pageNumber" to "int", "pageSize" to "int", "sort" to "org.springframework.data.domain.Sort",
                "offset" to "long", "paged" to "boolean", "unpaged" to "boolean",
            ),
            typesOf(pageable)
        )
        assertEquals(
            "Pageable.unpaged() is written as the string INSTANCE, so the object shape is assumed: $pageable",
            true, pageable["wireShapeAssumed"]?.asBoolean()
        )
    }

    private fun typesOf(schema: JsonNode): Map<String, String> =
        schema["fields"]?.associate { it["name"].asText() to it["type"].asText() }.orEmpty()

    private fun addWebConfigNamed(className: String, mode: String) {
        addFile(
            "com/example/config/$className.java", """
            package com.example.config;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Configuration
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.$mode)
            public class $className {
            }
            """.trimIndent()
        )
        assertResolves("com.example.config.$className")
    }

    fun testListOfDtoIsUnchanged() = runBlockingReleasingWriteIntent<Unit> {
        val schema = schemaOf("/orders/list", "java.util.List<$ORDER_DTO>")

        assertEquals(listOf("className", "fields"), keys(schema))
        assertEquals(ORDER_DTO, schema["className"].asText())
        assertEquals(listOf("id", "note"), names(schema))
    }

    fun testPlainDtoIsUnchanged() = runBlockingReleasingWriteIntent<Unit> {
        val schema = schemaOf("/orders/one", ORDER_DTO)

        assertEquals(listOf("className", "fields"), keys(schema))
        assertEquals(listOf("id", "note"), names(schema))
    }

    private companion object {
        const val CONFIG_PACKAGE = "com.example.config"
        const val WEB_CONFIG = "$CONFIG_PACKAGE.WebConfig"
        const val ALPHA_WEB_CONFIG = "$CONFIG_PACKAGE.AlphaWebConfig"
        const val COMPOSED_WEB_SUPPORT = "$CONFIG_PACKAGE.EnableComposedWeb"
        const val COMPOSED_WEB_CONFIG = "$CONFIG_PACKAGE.ComposedWebConfig"
        const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
        const val ALLOW_BEAN_DEFINITION_OVERRIDING_KEY = "spring.main.allow-bean-definition-overriding"
        const val VENDOR_WEB_CONFIG = "com.vendor.web.VendorWebConfig"
        const val VENDOR_AUTO_CONFIGURATION = "com.vendor.web.VendorWebAutoConfiguration"
        const val BOOT_DATA_WEB_AUTO_CONFIGURATION =
            "org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration"
        const val AUTO_CONFIGURATION_IMPORTS_PATH =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"

        fun vendorAutoConfigurationSource(mode: String) = """
            package com.vendor.web;

            import org.springframework.boot.autoconfigure.AutoConfiguration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @AutoConfiguration
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.$mode)
            public class VendorWebAutoConfiguration {
            }
        """.trimIndent()

        val VENDOR_WEB_CONFIG_SOURCE = """
            package com.vendor.web;

            import org.springframework.context.annotation.Configuration;
            import org.springframework.data.web.config.EnableSpringDataWebSupport;
            import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

            @Configuration
            @EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
            public class VendorWebConfig {
            }
        """.trimIndent()

        val VENDOR_IMPORTS_SOURCE = """
            package com.example.shop;

            import com.vendor.web.VendorWebConfig;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Import;

            @Configuration
            @Import(VendorWebConfig.class)
            public class VendorImports {
            }
        """.trimIndent()
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

    fun testPropertyIsIgnoredOnAReactiveApplication() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$SERIALIZATION_MODE_KEY=via-dto")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertSource("DEFAULT", schema)
        assertPageAssumed(schema)
    }

    fun testWebSupportAnnotationOnAReactiveApplicationIsUnknown() = runBlockingReleasingWriteIntent<Unit> {
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

    fun testPlainDtoIsUnchangedOnAReactiveApplication() = runBlockingReleasingWriteIntent<Unit> {
        val schema = schemaOf("/orders/one", ORDER_DTO)

        assertEquals(listOf("className", "fields"), keys(schema))
        assertEquals(listOf("id", "note"), names(schema))
    }
}
