/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import kotlinx.coroutines.runBlocking

/**
 * A Spring Data `Page` or `Slice` response in `explyt_get_spring_endpoint_contract` is described as Spring Data writes
 * it, not by its type argument.
 *
 * `PageImpl` is written through its getters (`DIRECT`) unless `@EnableSpringDataWebSupport(pageSerializationMode =
 * VIA_DTO)` or `spring.data.web.pageable.serialization-mode` re-wraps it into `PagedModel` (`content` + `page`).
 * `SliceImpl` is never re-wrapped. The schema names the shape in `wireShape` and where the mode was read in
 * `wireShapeSource` - `UNKNOWN` when it cannot be read, never a confident guess - and `wireShapeAssumed` says that the
 * declared interface was taken for its Spring Data implementation.
 */
class SpringBootApplicationMcpToolsetPageSchemaTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_4_0,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        SOURCES.forEach { (path, text) -> myFixture.addFileToProject(path, text.trimIndent()) }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    fun testPageWithoutWebSupportIsWrittenDirectByDefault() = runBlocking<Unit> {
        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertEquals("DEFAULT", schema["wireShapeSource"].asText())
        assertTrue("a declared Page is taken for PageImpl", schema["wireShapeAssumed"].asBoolean())
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
        assertEquals("@EnableSpringDataWebSupport on $WEB_CONFIG", schema["wireShapeSource"].asText())
    }

    fun testViaDtoAnnotationInKotlinWritesPagedModel() = runBlocking<Unit> {
        myFixture.addFileToProject(
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
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        assertResolves(ENABLE_WEB_SUPPORT)

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertEquals("@EnableSpringDataWebSupport on com.example.config.KotlinWebConfig", schema["wireShapeSource"].asText())
    }

    fun testExplicitDirectAnnotationWritesPageDirect() = runBlocking<Unit> {
        addWebConfig("DIRECT")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertEquals("@EnableSpringDataWebSupport on $WEB_CONFIG", schema["wireShapeSource"].asText())
    }

    fun testAnnotationWithoutModeAttributeIsDirect() = runBlocking<Unit> {
        addWebConfig(null)

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertEquals("@EnableSpringDataWebSupport on $WEB_CONFIG", schema["wireShapeSource"].asText())
    }

    fun testViaDtoPropertyWritesPagedModel() = runBlocking<Unit> {
        addProperties("spring.data.web.pageable.serialization-mode=via-dto")

        val schema = pageSchema("/orders/page")

        assertPagedModel(schema)
        assertEquals(SERIALIZATION_MODE_KEY, schema["wireShapeSource"].asText())
    }

    fun testAnnotationOutranksTheProperty() = runBlocking<Unit> {
        addWebConfig("DIRECT")
        addProperties("spring.data.web.pageable.serialization-mode=via-dto")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertEquals("@EnableSpringDataWebSupport on $WEB_CONFIG", schema["wireShapeSource"].asText())
    }

    fun testPlaceholderModeIsReportedUnknownWithTheDefaultShape() = runBlocking<Unit> {
        addProperties("spring.data.web.pageable.serialization-mode=\${PAGE_MODE}")

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertEquals("UNKNOWN", schema["wireShapeSource"].asText())
    }

    fun testProfileOnlyModeIsReportedUnknownWithTheDefaultShape() = runBlocking<Unit> {
        myFixture.addFileToProject("application-prod.properties", "spring.data.web.pageable.serialization-mode=via-dto")
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val schema = pageSchema("/orders/page")

        assertDirectPage(schema)
        assertEquals("UNKNOWN", schema["wireShapeSource"].asText())
    }

    fun testSliceIsWrittenDirect() = runBlocking<Unit> {
        val schema = schemaOf("/orders/slice", "$SLICE<$ORDER_DTO>")

        assertDirectSlice(schema)
    }

    fun testSliceStaysDirectUnderViaDto() = runBlocking<Unit> {
        addWebConfig("VIA_DTO")

        val schema = schemaOf("/orders/slice", "$SLICE<$ORDER_DTO>")

        assertDirectSlice(schema)
    }

    fun testResponseEntityOfPageUnwrapsToThePage() = runBlocking<Unit> {
        val schema = schemaOf("/orders/entity-page", "org.springframework.http.ResponseEntity<$PAGE<$ORDER_DTO>>")

        assertDirectPage(schema)
        assertEquals("DEFAULT", schema["wireShapeSource"].asText())
    }

    fun testKotlinSuspendHandlerReturningPageIsThePage() = runBlocking<Unit> {
        myFixture.addFileToProject(
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
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val schema = schemaOf("/orders/suspend-page", "$PAGE<$ORDER_DTO>")

        assertDirectPage(schema)
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

    private suspend fun pageSchema(url: String): JsonNode = schemaOf(url, "$PAGE<$ORDER_DTO>")

    private suspend fun schemaOf(url: String, declaredReturnType: String): JsonNode {
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

    private fun assertDirectPage(schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.domain.PageImpl", schema["className"]?.asText())
        assertEquals("PAGE_DIRECT", schema["wireShape"]?.asText())
        assertEquals(PAGE_DIRECT_KEYS, names(schema).toSet())
        assertEquals(ORDER_DTO, field(schema, "content")["nested"]["className"].asText())
    }

    private fun assertDirectSlice(schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.domain.SliceImpl", schema["className"]?.asText())
        assertEquals("SLICE_DIRECT", schema["wireShape"]?.asText())
        assertEquals(SLICE_DIRECT_KEYS, names(schema).toSet())
        assertEquals(ORDER_DTO, field(schema, "content")["nested"]["className"].asText())
    }

    private fun assertPagedModel(schema: JsonNode) {
        assertEquals(schema.toString(), "org.springframework.data.web.PagedModel", schema["className"]?.asText())
        assertEquals("PAGED_MODEL", schema["wireShape"]?.asText())
        assertEquals(setOf("content", "page"), names(schema).toSet())
        assertEquals(ORDER_DTO, field(schema, "content")["nested"]["className"].asText())
        assertEquals(
            setOf("size", "number", "totalElements", "totalPages"),
            names(field(schema, "page")["nested"]).toSet()
        )
    }

    private fun addWebConfig(mode: String?) {
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

    private fun addProperties(text: String) {
        myFixture.addFileToProject("application.properties", text)
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun assertResolves(fqn: String) {
        assertNotNull(
            "the fixture must resolve $fqn, or the test proves nothing",
            JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
        )
    }

    private fun keys(schema: JsonNode): List<String> = schema.fieldNames().asSequence().toList()

    private fun names(schema: JsonNode): List<String> = schema["fields"]?.map { it["name"].asText() }.orEmpty()

    private fun field(schema: JsonNode, name: String): JsonNode =
        schema["fields"]?.singleOrNull { it["name"].asText() == name }
            ?: throw AssertionError("no field '$name' in $schema")

    private companion object {
        const val PAGE = "org.springframework.data.domain.Page"
        const val SLICE = "org.springframework.data.domain.Slice"
        const val ENABLE_WEB_SUPPORT = "org.springframework.data.web.config.EnableSpringDataWebSupport"
        const val ORDER_DTO = "com.example.shop.OrderDto"
        const val WEB_CONFIG = "com.example.config.WebConfig"
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
