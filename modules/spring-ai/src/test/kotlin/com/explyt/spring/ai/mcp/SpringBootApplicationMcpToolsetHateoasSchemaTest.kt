/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.explyt.spring.web.util.WebApplicationStack
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
 *
 * The shape is only assumed where the run differs: with `spring.hateoas.use-hal-as-default-json-media-type=false`
 * Boot 3 writes plain JSON and Boot 4 answers `406` for `application/json`, and Evo Inflector on the classpath
 * pluralises the default collection relation (`orderDtoes`) while a declared `@Relation` still wins.
 */
abstract class HateoasSchemaTestCase : PageSchemaTestCase() {

    override val stack = WebApplicationStack.SERVLET

    override fun setUp() {
        super.setUp()
        HAL_SOURCES.forEach { (path, text) -> myFixture.addFileToProject(path, text.trimIndent()) }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    protected fun assertHalEntity(schema: JsonNode) {
        assertEquals(schema.toString(), ENTITY_KEYS, names(schema).toSet())
    }

    protected fun assertHalCollection(schema: JsonNode, relation: String, extraKeys: Set<String> = emptySet()) {
        assertEquals(schema.toString(), setOf("_embedded", "_links") + extraKeys, names(schema).toSet())
        val embedded = field(schema, "_embedded").path("nested")
        assertEquals(schema.toString(), setOf(relation), names(embedded).toSet())
        assertEquals(schema.toString(), ENTITY_KEYS, names(field(embedded, relation).path("nested")).toSet())
    }

    protected fun assertHalAssumed(schema: JsonNode) {
        assertEquals("the HAL shape is assumed: $schema", true, schema["wireShapeAssumed"]?.asBoolean())
    }

    protected fun assertHalNotAssumed(schema: JsonNode) {
        assertNull("the HAL default is not an assumption: $schema", schema["wireShapeAssumed"])
    }

    protected fun assertFromLibrary(fqn: String, jar: String) {
        val file = classOf(fqn).containingFile.virtualFile
        assertTrue("$fqn must come from a library: $file", ProjectFileIndex.getInstance(project).isInLibraryClasses(file))
        assertTrue("$fqn must come from $jar: $file", jar in file.path)
    }

    protected fun assertFromHateoasLibrary(fqn: String) = assertFromLibrary(fqn, HATEOAS_JAR)

    protected fun assertWithoutRelation(fqn: String) {
        assertFromHateoasLibrary(RELATION)
        assertNull("$fqn must take the default collection relation", relationOf(fqn))
    }

    protected fun assertDeclaresItemsRelation(fqn: String) {
        assertFromHateoasLibrary(RELATION)
        assertEquals(
            "the element type declares its collection relation",
            "\"items\"",
            relationOf(fqn)?.findDeclaredAttributeValue("collectionRelation")?.text
        )
    }

    protected fun evoInflector(): PsiClass? =
        JavaPsiFacade.getInstance(project).findClass(EVO_INFLECTOR, GlobalSearchScope.allScope(project))

    private fun relationOf(fqn: String) = classOf(fqn).getAnnotation(RELATION)

    private fun classOf(fqn: String): PsiClass =
        JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))
            ?: throw AssertionError("the fixture must resolve $fqn, or the test proves nothing")

    protected companion object {
        const val HATEOAS_JAR = "spring-hateoas-2.5.3.jar"
        const val EVO_INFLECTOR_JAR = "evo-inflector-1.3.jar"
        const val ENTITY_MODEL = "org.springframework.hateoas.EntityModel"
        const val COLLECTION_MODEL = "org.springframework.hateoas.CollectionModel"
        const val HATEOAS_PAGED_MODEL = "org.springframework.hateoas.PagedModel"
        const val RELATION = "org.springframework.hateoas.server.core.Relation"
        const val EVO_INFLECTOR = "org.atteo.evo.inflector.English"
        const val RESPONSE_ENTITY = "org.springframework.http.ResponseEntity"
        const val HAL_ORDER_DTO = "com.example.hal.OrderDto"
        const val RELATED_ORDER_DTO = "com.example.hal.related.OrderDto"
        const val KOTLIN_ORDER_DTO = "com.example.halkt.OrderDto"
        const val HAL_DEFAULT_KEY = "spring.hateoas.use-hal-as-default-json-media-type"
        const val SHAPE = "com.example.hal.shape.Shape"
        const val OBJECT_NODE = "com.fasterxml.jackson.databind.node.ObjectNode"
        const val PROBLEM_DETAIL = "org.springframework.http.ProblemDetail"

        val SHAPE_SOURCES = mapOf(
            "com/example/hal/shape/Shape.java" to """
                package com.example.hal.shape;

                import com.fasterxml.jackson.annotation.JsonSubTypes;
                import com.fasterxml.jackson.annotation.JsonTypeInfo;

                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
                @JsonSubTypes({
                    @JsonSubTypes.Type(value = Circle.class, name = "circle"),
                    @JsonSubTypes.Type(value = Square.class, name = "square")
                })
                public abstract class Shape {
                    public String label;
                }
            """,
            "com/example/hal/shape/Circle.java" to """
                package com.example.hal.shape;

                public class Circle extends Shape { public double radius; }
            """,
            "com/example/hal/shape/Square.java" to """
                package com.example.hal.shape;

                public class Square extends Shape { public double side; }
            """,
        )

        val ENTITY_KEYS = setOf("id", "name", "_links")

        val HATEOAS_LIBRARIES = arrayOf(
            TestLibrary.springBootAutoConfigure_3_1_1,
            TestLibrary.springWebMvc_6_0_7,
            TestLibrary.springDataJpa_3_4_0,
            TestLibrary.jacksonAnnotations_2_15_2,
            TestLibrary.jacksonDatabind_2_15_2,
            TestLibrary.kotlin_1_9_22,
            TestLibrary.springHateoas_2_5_3,
        )

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

class SpringBootApplicationMcpToolsetHateoasSchemaTest : HateoasSchemaTestCase() {

    override val libraries: Array<TestLibrary> = HATEOAS_LIBRARIES

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
        assertNull("no Evo Inflector pluralises the default relation", evoInflector())

        val schema = schemaOf("/hal/collection", "$COLLECTION_MODEL<$ENTITY_MODEL<$HAL_ORDER_DTO>>")

        assertHalCollection(schema, "orderDtoList")
    }

    fun testCollectionModelEmbedsItemsUnderTheDeclaredCollectionRelation() = runBlockingReleasingWriteIntent<Unit> {
        assertFromHateoasLibrary(COLLECTION_MODEL)
        assertDeclaresItemsRelation(RELATED_ORDER_DTO)

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

    fun testHalNotTheDefaultJsonMediaTypeMakesTheShapeAssumed() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$HAL_DEFAULT_KEY=false")

        val schema = schemaOf("/hal/entity", "$ENTITY_MODEL<$HAL_ORDER_DTO>")

        assertHalEntity(schema)
        assertHalAssumed(schema)
    }

    fun testUnresolvableHalDefaultMakesTheShapeAssumed() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$HAL_DEFAULT_KEY=\${UNSET_HAL}")

        val schema = schemaOf("/hal/entity", "$ENTITY_MODEL<$HAL_ORDER_DTO>")

        assertHalEntity(schema)
        assertHalAssumed(schema)
    }

    fun testExplicitHalDefaultIsNotAnAssumption() = runBlockingReleasingWriteIntent<Unit> {
        addFile("application.properties", "$HAL_DEFAULT_KEY=true")

        val schema = schemaOf("/hal/entity", "$ENTITY_MODEL<$HAL_ORDER_DTO>")

        assertHalEntity(schema)
        assertHalNotAssumed(schema)
    }

    fun testEntityModelKeepsThePolymorphismOfItsContent() = runBlockingReleasingWriteIntent<Unit> {
        addContentController(SHAPE, SHAPE_SOURCES)
        val plain = schemaOf("/content/plain", SHAPE)
        assertEquals("the plain contract is polymorphic: $plain", true, plain["polymorphic"]?.asBoolean())
        assertTrue("the plain contract has a discriminator: $plain", plain.hasNonNull("discriminator"))
        assertEquals("the plain contract lists both variants: $plain", 2, plain["variants"]?.size())

        val entity = schemaOf("/content/entity", "$ENTITY_MODEL<$SHAPE>")

        assertContentKept(plain, entity)
    }

    fun testEntityModelKeepsTheOmittedSchemaOfItsContent() = runBlockingReleasingWriteIntent<Unit> {
        addContentController(OBJECT_NODE)
        val plain = schemaOf("/content/plain", OBJECT_NODE)
        assertEquals("the plain contract omits a JSON tree: $plain", "JSON_TREE", plain["schemaOmitted"]?.asText())

        val entity = schemaOf("/content/entity", "$ENTITY_MODEL<$OBJECT_NODE>")

        assertEquals(entity.toString(), "JSON_TREE", entity["schemaOmitted"]?.asText())
        assertTrue("HATEOAS writes its links: $entity", "_links" in names(entity))
    }

    fun testEntityModelKeepsTheAdditionalPropertiesOfItsContent() = runBlockingReleasingWriteIntent<Unit> {
        addContentController(PROBLEM_DETAIL)
        val plain = schemaOf("/content/plain", PROBLEM_DETAIL)
        assertEquals("the plain contract writes any-getter entries: $plain", true, plain["additionalProperties"]?.asBoolean())

        val entity = schemaOf("/content/entity", "$ENTITY_MODEL<$PROBLEM_DETAIL>")

        assertContentKept(plain, entity)
    }

    private fun addContentController(content: String, sources: Map<String, String> = emptyMap()) {
        sources.forEach { (path, text) -> myFixture.addFileToProject(path, text.trimIndent()) }
        addFile(
            "com/example/hal/ContentController.java", """
            package com.example.hal;

            import org.springframework.hateoas.EntityModel;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class ContentController {
                @GetMapping("/content/plain")
                public $content plain() { return null; }

                @GetMapping("/content/entity")
                public EntityModel<$content> entity() { return null; }
            }
            """.trimIndent()
        )
    }

    private fun assertContentKept(plain: JsonNode, entity: JsonNode) {
        assertTrue("HATEOAS writes its links: $entity", "_links" in names(entity))
        assertEquals("the content schema survives the EntityModel", withoutClassName(plain), withoutHal(entity))
    }

    private fun withoutClassName(schema: JsonNode): ObjectNode =
        (schema.deepCopy<JsonNode>() as ObjectNode).apply { remove("className") }

    private fun withoutHal(schema: JsonNode): ObjectNode = withoutClassName(schema).apply {
        val fields = get("fields") as? ArrayNode ?: return@apply
        fields.indexOfFirst { it["name"].asText() == "_links" }.takeIf { it >= 0 }?.let(fields::remove)
    }
}

class SpringBootApplicationMcpToolsetEvoInflectorHateoasSchemaTest : HateoasSchemaTestCase() {

    override val libraries: Array<TestLibrary> = HATEOAS_LIBRARIES + TestLibrary.evoInflector_1_3

    fun testInflectedDefaultRelationIsNotGuessed() = runBlockingReleasingWriteIntent<Unit> {
        assertFromLibrary(EVO_INFLECTOR, EVO_INFLECTOR_JAR)
        assertWithoutRelation(HAL_ORDER_DTO)

        val schema = schemaOf("/hal/collection", "$COLLECTION_MODEL<$ENTITY_MODEL<$HAL_ORDER_DTO>>")

        assertEquals(schema.toString(), setOf("_embedded", "_links"), names(schema).toSet())
        assertTrue(
            "the pluralised relation is not derived, so _embedded names no key: $schema",
            field(schema, "_embedded").path("nested").isNull
        )
        assertHalAssumed(schema)
    }

    fun testDeclaredRelationWinsOverEvoInflector() = runBlockingReleasingWriteIntent<Unit> {
        assertFromLibrary(EVO_INFLECTOR, EVO_INFLECTOR_JAR)
        assertDeclaresItemsRelation(RELATED_ORDER_DTO)

        val schema = schemaOf("/hal/relation", "$COLLECTION_MODEL<$ENTITY_MODEL<$RELATED_ORDER_DTO>>")

        assertHalCollection(schema, "items")
        assertHalNotAssumed(schema)
    }
}
