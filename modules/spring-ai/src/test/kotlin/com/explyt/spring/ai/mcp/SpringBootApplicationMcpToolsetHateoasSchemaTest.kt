/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.explyt.spring.web.util.WebApplicationStack
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil

/**
 * A Spring HATEOAS model in `explyt_get_spring_endpoint_contract` is described by the HAL document Boot writes for it,
 * not by its type argument.
 *
 * Run-verified on Boot 3.5.15 (spring-hateoas 2.5.3) and Boot 4.1.0 (spring-hateoas 3.1.1), HAL being the default for
 * any `Accept` and for `application/json`: `EntityModel<T>` writes the fields of `T` inline next to `_links`;
 * `CollectionModel<T>` writes `_embedded` keyed by the collection relation of the element type - its
 * `@Relation(collectionRelation)`, else `uncapitalize(simpleName) + "List"` - next to `_links`; the HATEOAS `PagedModel`
 * adds `page` with `size`, `totalElements`, `totalPages` and `number`.
 */
class SpringBootApplicationMcpToolsetHateoasSchemaTest : PageSchemaTestCase() {

    override val stack = WebApplicationStack.SERVLET

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.springDataJpa_3_4_0,
        TestLibrary.jacksonAnnotations_2_15_2,
        TestLibrary.jacksonDatabind_2_15_2,
        TestLibrary.kotlin_1_9_22,
        TestLibrary.springHateoas_2_5_3,
    )

    override fun setUp() {
        super.setUp()
        HAL_SOURCES.forEach { (path, text) -> myFixture.addFileToProject(path, text.trimIndent()) }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    fun testEntityModelWritesTheContentInlineNextToLinks() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(ENTITY_MODEL)

        val schema = schemaOf("/hal/entity", "$ENTITY_MODEL<$HAL_ORDER_DTO>")

        assertHalEntity(schema)
    }

    fun testResponseEntityOfEntityModelWritesTheHalEntity() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(ENTITY_MODEL)

        val schema = schemaOf("/hal/response", "$RESPONSE_ENTITY<$ENTITY_MODEL<$HAL_ORDER_DTO>>")

        assertHalEntity(schema)
    }

    fun testCollectionModelEmbedsItemsUnderTheDefaultCollectionRelation() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(COLLECTION_MODEL)
        assertWithoutRelation(HAL_ORDER_DTO)

        val schema = schemaOf("/hal/collection", "$COLLECTION_MODEL<$ENTITY_MODEL<$HAL_ORDER_DTO>>")

        assertHalCollection(schema, "orderDtoList")
    }

    fun testCollectionModelEmbedsItemsUnderTheDeclaredCollectionRelation() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(COLLECTION_MODEL)
        assertFromHateoasLibrary(RELATION)
        assertEquals(
            "the element type declares its collection relation",
            "\"items\"",
            relationOf(RELATED_ORDER_DTO)?.findDeclaredAttributeValue("collectionRelation")?.text
        )

        val schema = schemaOf("/hal/relation", "$COLLECTION_MODEL<$ENTITY_MODEL<$RELATED_ORDER_DTO>>")

        assertHalCollection(schema, "items")
    }

    fun testHateoasPagedModelEmbedsItemsNextToLinksAndPageMetadata() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(HATEOAS_PAGED_MODEL)
        assertWithoutRelation(HAL_ORDER_DTO)

        val schema = schemaOf("/hal/paged", "$HATEOAS_PAGED_MODEL<$ENTITY_MODEL<$HAL_ORDER_DTO>>")

        assertHalCollection(schema, "orderDtoList", extraKeys = setOf("page"))
        assertEquals(
            schema.toString(),
            setOf("size", "totalElements", "totalPages", "number"),
            names(field(schema, "page").path("nested")).toSet()
        )
    }

    fun testKotlinEntityModelWritesTheContentInlineNextToLinks() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(ENTITY_MODEL)

        val schema = schemaOf("/hal/kotlin-entity", "$ENTITY_MODEL<$KOTLIN_ORDER_DTO>")

        assertHalEntity(schema)
    }

    fun testKotlinCollectionModelEmbedsItemsUnderTheDefaultCollectionRelation() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(COLLECTION_MODEL)
        assertWithoutRelation(KOTLIN_ORDER_DTO)

        val schema = schemaOf("/hal/kotlin-collection", "$COLLECTION_MODEL<$ENTITY_MODEL<$KOTLIN_ORDER_DTO>>")

        assertHalCollection(schema, "orderDtoList")
    }

    private fun assertHalEntity(schema: JsonNode) {
        assertEquals(schema.toString(), ENTITY_KEYS, names(schema).toSet())
    }

    private fun assertHalCollection(schema: JsonNode, relation: String, extraKeys: Set<String> = emptySet()) {
        assertEquals(schema.toString(), setOf("_embedded", "_links") + extraKeys, names(schema).toSet())
        val embedded = field(schema, "_embedded").path("nested")
        assertEquals(schema.toString(), setOf(relation), names(embedded).toSet())
        assertEquals(schema.toString(), ENTITY_KEYS, names(field(embedded, relation).path("nested")).toSet())
    }

    private fun assertFromHateoasLibrary(fqn: String) {
        val file = classOf(fqn).containingFile.virtualFile
        assertTrue("$fqn must come from a library: $file", ProjectFileIndex.getInstance(project).isInLibraryClasses(file))
        assertTrue("$fqn must come from spring-hateoas 2.5.3: $file", HATEOAS_JAR in file.path)
    }

    private fun assertWithoutRelation(fqn: String) {
        assertFromHateoasLibrary(RELATION)
        assertNull("$fqn must take the default collection relation", relationOf(fqn))
    }

    private fun relationOf(fqn: String) = classOf(fqn).getAnnotation(RELATION)

    private fun classOf(fqn: String): PsiClass =
        JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
            ?: throw AssertionError("the fixture must resolve $fqn, or the test proves nothing")

    private companion object {
        const val HATEOAS_JAR = "spring-hateoas-2.5.3.jar"
        const val ENTITY_MODEL = "org.springframework.hateoas.EntityModel"
        const val COLLECTION_MODEL = "org.springframework.hateoas.CollectionModel"
        const val HATEOAS_PAGED_MODEL = "org.springframework.hateoas.PagedModel"
        const val RELATION = "org.springframework.hateoas.server.core.Relation"
        const val RESPONSE_ENTITY = "org.springframework.http.ResponseEntity"
        const val HAL_ORDER_DTO = "com.example.hal.OrderDto"
        const val RELATED_ORDER_DTO = "com.example.hal.related.OrderDto"
        const val KOTLIN_ORDER_DTO = "com.example.halkt.OrderDto"

        val ENTITY_KEYS = setOf("id", "name", "_links")

        val HAL_SOURCES = mapOf(
            "com/example/hal/OrderDto.java" to """
                package com.example.hal;

                public class OrderDto {
                    private long id;
                    private String name;

                    public long getId() { return id; }
                    public String getName() { return name; }
                }
            """,
            "com/example/hal/related/OrderDto.java" to """
                package com.example.hal.related;

                import org.springframework.hateoas.server.core.Relation;

                @Relation(collectionRelation = "items")
                public class OrderDto {
                    private long id;
                    private String name;

                    public long getId() { return id; }
                    public String getName() { return name; }
                }
            """,
            "com/example/hal/HalController.java" to """
                package com.example.hal;

                import org.springframework.hateoas.CollectionModel;
                import org.springframework.hateoas.EntityModel;
                import org.springframework.hateoas.PagedModel;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class HalController {
                    @GetMapping("/hal/entity")
                    public EntityModel<OrderDto> entity() { return null; }

                    @GetMapping("/hal/response")
                    public ResponseEntity<EntityModel<OrderDto>> response() { return null; }

                    @GetMapping("/hal/collection")
                    public CollectionModel<EntityModel<OrderDto>> collection() { return null; }

                    @GetMapping("/hal/relation")
                    public CollectionModel<EntityModel<com.example.hal.related.OrderDto>> relation() { return null; }

                    @GetMapping("/hal/paged")
                    public PagedModel<EntityModel<OrderDto>> paged() { return null; }
                }
            """,
            "com/example/halkt/KotlinHalController.kt" to """
                package com.example.halkt

                import org.springframework.hateoas.CollectionModel
                import org.springframework.hateoas.EntityModel
                import org.springframework.web.bind.annotation.GetMapping
                import org.springframework.web.bind.annotation.RestController

                class OrderDto(val id: Long, val name: String)

                @RestController
                class KotlinHalController {
                    @GetMapping("/hal/kotlin-entity")
                    fun entity(): EntityModel<OrderDto> = TODO()

                    @GetMapping("/hal/kotlin-collection")
                    fun collection(): CollectionModel<EntityModel<OrderDto>> = TODO()
                }
            """,
        )
    }
}
