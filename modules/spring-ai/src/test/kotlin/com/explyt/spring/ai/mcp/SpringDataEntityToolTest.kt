/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * The entity tool answers a paged inventory rather than every entity's schema at once.
 *
 * What is asserted here is the contract a client parses: a page carries its own count and continuation, a
 * compact record is genuinely without a schema, and the budget is spent on the finished document rather than
 * on a number of entities.
 */
class SpringDataEntityToolTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.jakarta_persistence_3_1_0,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    private fun projectPath(): String = project.basePath ?: ""

    private fun parse(json: String): JsonNode = mapper.readTree(json)

    private fun classNames(page: JsonNode): List<String> =
        page["entities"].map { it["className"].asText() }

    /**
     * A bare array cannot say how many entities exist, so a caller could not tell a complete inventory from a
     * capped one - which is what the old 500-entity cutoff silently produced.
     */
    fun testTheInventoryCarriesItsOwnCountAndContinuation() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val page = parse(toolset.getSpringDataEntities(projectPath = projectPath()))

        assertEquals("OK", page["status"].asText())
        assertEquals("The fixture declares five entities", 5, page["totalCount"].asInt())
        assertEquals(0, page["offset"].asInt())
        assertFalse("A page holding every entity does not continue", page["truncated"].asBoolean())
        assertTrue("A terminated continuation is null, not absent", page["nextOffset"].isNull)
        assertTrue("A page is versioned so it can be continued", page["revision"].asText().isNotEmpty())
        assertEquals(5, page["entities"].size())
    }

    /** Every entity has to stay reachable, or a caller cannot tell a page boundary from a missing entity. */
    fun testASecondPageReturnsTheEntitiesTheFirstOneLeft() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val first = parse(toolset.getSpringDataEntities(projectPath = projectPath(), limit = 2))
        assertTrue("Precondition: the fixture must hold more entities than one page", first["truncated"].asBoolean())
        assertEquals(2, first["entities"].size())
        assertEquals(2, first["nextOffset"].asInt())

        val second = parse(
            toolset.getSpringDataEntities(
                projectPath = projectPath(),
                limit = 2,
                offset = first["nextOffset"].asInt(),
                expectedRevision = first["revision"].asText()
            )
        )

        assertEquals(2, second["offset"].asInt())
        val seen = classNames(first) + classNames(second)
        assertEquals("The two pages must not overlap", seen.size, seen.toSet().size)
        assertTrue(
            "Paging must reach entities the first page did not carry, got $seen",
            classNames(second).none { it in classNames(first) }
        )
    }

    /** Continuing a page of an answer that may have changed would serve a slice of a different result set. */
    fun testAContinuationWithoutTheRevisionIsRejected() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val page = parse(toolset.getSpringDataEntities(projectPath = projectPath(), offset = 2, limit = 2))

        assertEquals("ERROR", page["status"].asText())
        assertEquals("INVALID_ARGUMENT", page["error"]["code"].asText())
    }

    /**
     * The inventory exists to be selected from without paying for schemas, and a client that sees `fields`
     * cannot tell an entity with no column from one whose schema was never read.
     */
    fun testTheInventoryOmitsTheSchemaThatDetailsCarry() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val compact = parse(
            toolset.getSpringDataEntities(projectPath = projectPath(), className = DEMO_ENTITY)
        )["entities"][0]
        val detailed = parse(
            toolset.getSpringDataEntities(
                projectPath = projectPath(),
                className = DEMO_ENTITY,
                includeDetails = true
            )
        )["entities"][0]

        assertTrue(
            "Precondition: the entity must declare fields, or omitting them proves nothing",
            detailed["fields"].size() > 0
        )
        assertEquals(compact["className"].asText(), detailed["className"].asText())
        assertFalse("An inventory record must not carry fields", compact.has("fields"))
        assertFalse("An inventory record must not carry indexes", compact.has("indexes"))
    }

    /** Details are requested by identity, so a caller asking about one entity is not served the package. */
    fun testAnExactClassNameSelectsOneEntity() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val page = parse(toolset.getSpringDataEntities(projectPath = projectPath(), className = DEMO_ENTITY))

        assertEquals(1, page["totalCount"].asInt())
        assertEquals(listOf(DEMO_ENTITY), classNames(page))
    }

    /** Two ways to narrow the same choice contradict each other; guessing which one wins would be worse. */
    fun testAClassNameAndAPackageFilterAreRejectedTogether() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val page = parse(
            toolset.getSpringDataEntities(
                projectPath = projectPath(),
                packageFilter = "com.example.app",
                className = DEMO_ENTITY
            )
        )

        assertEquals("ERROR", page["status"].asText())
        assertEquals("INVALID_ARGUMENT", page["error"]["code"].asText())
    }

    /** An entity that does not exist is an empty answer, not a failure: it may simply not be written yet. */
    fun testAnUnknownClassNameIsAnEmptyAnswerRatherThanAnError() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val page = parse(
            toolset.getSpringDataEntities(projectPath = projectPath(), className = "com.example.app.entity.Absent")
        )

        assertEquals("OK", page["status"].asText())
        assertEquals(0, page["totalCount"].asInt())
        assertEquals(0, page["entities"].size())
        assertTrue("An exhausted answer does not continue", page["nextOffset"].isNull)
    }

    /** An entity without `@Table` still maps to a table - the one named after its class. */
    fun testAnEntityWithoutATableAnnotationFallsBackToItsClassName() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val page = parse(
            toolset.getSpringDataEntities(
                projectPath = projectPath(),
                className = "com.example.app.entity.ShipmentEntity"
            )
        )

        assertEquals(1, page["totalCount"].asInt())
        assertEquals("ShipmentEntity", page["entities"][0]["tableName"].asText())
    }

    /**
     * The budget is what the client receives, so it is measured on the finished document - metadata, escaping
     * and continuation included. A cap on entities says nothing about the size of one whose package is deep
     * and whose column names are long, which is why a page can end before `limit` is reached.
     */
    fun testTheWholeAnswerStaysWithinItsBudgetAndContinuesFromThere() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val json = toolset.getSpringDataEntities(projectPath = projectPath(), maxChars = 700)
        val page = parse(json)

        assertEquals("OK", page["status"].asText())
        assertTrue("The finished answer must fit its budget, got " + json.length, json.length <= 700)
        assertTrue(
            "Precondition: the budget must bind before the entities run out, or size proves nothing",
            page["entities"].size() < page["totalCount"].asInt()
        )
        assertTrue("A shortened page continues", page["truncated"].asBoolean())
        assertEquals(page["entities"].size(), page["nextOffset"].asInt())
    }

    /** The entity whose package is deep and whose columns are long is the one a count-based cap mismeasures. */
    fun testALongEntityIsMeasuredByItsCharactersNotItsCount() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val json = toolset.getSpringDataEntities(projectPath = projectPath(), className = WAREHOUSE_ENTITY)
        val page = parse(json)

        assertEquals(1, page["totalCount"].asInt())
        assertTrue(
            "Precondition: this entity must be the long one, or the measurement proves nothing",
            page["entities"][0]["className"].asText().length > 60
        )
        assertTrue("Even one long entity must fit the default budget, got " + json.length, json.length <= 1800)
    }

    fun testTheDefaultAnswerFitsAClientContentArray() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val json = toolset.getSpringDataEntities(projectPath = projectPath())
        val page = parse(json)

        assertEquals("OK", page["status"].asText())
        assertTrue("Precondition: the answer must carry entities", page["entities"].size() > 0)

        val wrapped = mapper.writeValueAsString(mapper.createArrayNode().add(page))
        assertTrue("A wrapped default answer must stay under 2000 chars, got " + wrapped.length, wrapped.length <= 2000)
    }

    fun testAnEntityThatCannotFitIsReportedRatherThanTruncated() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val page = parse(
            toolset.getSpringDataEntities(
                projectPath = projectPath(),
                className = WAREHOUSE_ENTITY,
                includeDetails = true,
                maxChars = 512
            )
        )

        assertEquals("ERROR", page["status"].asText())
        assertEquals("RESPONSE_TOO_LARGE", page["error"]["code"].asText())
        assertTrue(
            "An overflow must name the budget that would fetch the record",
            page["error"]["message"].asText().contains(Regex("""\d{3,}"""))
        )
    }

    fun testAPrimaryKeyColumnIsNotNullableWhateverItsPropertyType() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/pk/Cluster.java", """
            package com.example.pk;

            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;

            @Entity
            public class Cluster {
                @Id
                private Integer id;

                private String region;

                @Column(nullable = false)
                private String code;
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.pk.Cluster")

        assertEquals(true, field(fields, "id")["primaryKey"].booleanValue())
        assertEquals("A primary-key column cannot hold NULL", false, field(fields, "id")["nullable"].booleanValue())
        assertEquals("An unannotated reference column stays nullable", true, field(fields, "region")["nullable"].booleanValue())
        assertEquals("A declared non-null column stays so", false, field(fields, "code")["nullable"].booleanValue())
    }

    fun testAKotlinNullableIdDeclaredNullableIsStillANotNullableColumn() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/pk/Node.kt", """
            package com.example.pk

            import jakarta.persistence.Column
            import jakarta.persistence.Entity
            import jakarta.persistence.Id

            @Entity
            class Node {
                @Id
                @Column(nullable = true)
                var id: Long? = null

                var label: String? = null
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.pk.Node")

        assertEquals("java.lang.Long", field(fields, "id")["type"].asText())
        assertEquals("The id is null only before the entity is persisted", false, field(fields, "id")["nullable"].booleanValue())
        assertEquals(true, field(fields, "label")["nullable"].booleanValue())
    }

    fun testAnEmbeddedIdIsANotNullablePrimaryKey() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/pk/ShardKey.java", """
            package com.example.pk;

            import jakarta.persistence.Embeddable;

            @Embeddable
            public class ShardKey {
                private Integer region;
                private Integer index;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/pk/Shard.java", """
            package com.example.pk;

            import jakarta.persistence.EmbeddedId;
            import jakarta.persistence.Entity;

            @Entity
            public class Shard {
                @EmbeddedId
                private ShardKey key;

                private String owner;
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.pk.Shard")

        assertEquals(true, field(fields, "key")["primaryKey"].booleanValue())
        assertEquals(false, field(fields, "key")["nullable"].booleanValue())
        assertEquals(true, field(fields, "owner")["nullable"].booleanValue())
    }

    fun testAnIdInheritedFromAMappedSuperclassIsANotNullablePrimaryKey() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/pk/Audited.java", """
            package com.example.pk;

            import jakarta.persistence.Id;
            import jakarta.persistence.MappedSuperclass;

            @MappedSuperclass
            public abstract class Audited {
                @Id
                private Long id;

                private String createdBy;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/pk/Event.java", """
            package com.example.pk;

            import jakarta.persistence.Entity;

            @Entity
            public class Event extends Audited {
                private String kind;
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.pk.Event")

        assertEquals(true, field(fields, "id")["primaryKey"].booleanValue())
        assertEquals(false, field(fields, "id")["nullable"].booleanValue())
        assertEquals(true, field(fields, "createdBy")["nullable"].booleanValue())
    }

    fun testJavaQuotedNamesAreReportedWithoutTheirDelimitersAndFlagged() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/quoted/Cluster.java", """
            package com.example.quoted;

            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.ManyToOne;
            import jakarta.persistence.Table;

            @Entity
            @Table(name = "`Cluster`")
            public class Cluster {
                @Id
                @Column(name = "\"ClusterId\"")
                private Long id;

                @ManyToOne
                @JoinColumn(name = "`OwnerId`")
                private Cluster owner;

                @Column(name = "region")
                private String region;
            }
            """.trimIndent()
        )

        val entity = detailedRecordOf("com.example.quoted.Cluster")
        val fields = entity["fields"]

        assertEquals("Cluster", entity["tableName"].asText())
        assertEquals(true, entity["tableQuoted"].booleanValue())
        assertEquals("ClusterId", field(fields, "id")["column"].asText())
        assertEquals(true, field(fields, "id")["columnQuoted"].booleanValue())
        assertEquals("OwnerId", field(fields, "owner")["joinColumn"].asText())
        assertEquals(true, field(fields, "owner")["joinColumnQuoted"].booleanValue())
        assertEquals("region", field(fields, "region")["column"].asText())
        assertFalse("An unquoted column carries no flag", field(fields, "region").has("columnQuoted"))
        assertFalse("A field without a join column carries no flag", field(fields, "region").has("joinColumnQuoted"))
    }

    fun testKotlinQuotedNamesAreReportedWithoutTheirDelimitersAndFlagged() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/quoted/Node.kt", """
            package com.example.quoted

            import jakarta.persistence.Column
            import jakarta.persistence.Entity
            import jakarta.persistence.Id
            import jakarta.persistence.JoinColumn
            import jakarta.persistence.ManyToOne
            import jakarta.persistence.Table

            @Entity
            @Table(name = "\"Node\"")
            class Node {
                @Id
                @Column(name = "`NodeId`")
                var id: Long? = null

                @ManyToOne
                @JoinColumn(name = "\"ParentId\"")
                var parent: Node? = null

                @Column(name = "label")
                var label: String? = null
            }
            """.trimIndent()
        )

        val entity = detailedRecordOf("com.example.quoted.Node")
        val fields = entity["fields"]

        assertEquals("Node", entity["tableName"].asText())
        assertEquals(true, entity["tableQuoted"].booleanValue())
        assertEquals("NodeId", field(fields, "id")["column"].asText())
        assertEquals(true, field(fields, "id")["columnQuoted"].booleanValue())
        assertEquals("ParentId", field(fields, "parent")["joinColumn"].asText())
        assertEquals(true, field(fields, "parent")["joinColumnQuoted"].booleanValue())
        assertFalse("An unquoted column carries no flag", field(fields, "label").has("columnQuoted"))
    }

    fun testAQuotedNameReferencedThroughAConstantIsReadLikeALiteral() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/quoted/Names.java", """
            package com.example.quoted;

            public final class Names {
                public static final String TABLE = "\"Segment\"";
                public static final String KEY = "`SegmentId`";
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/quoted/Segment.java", """
            package com.example.quoted;

            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.Table;

            @Entity
            @Table(name = Names.TABLE)
            public class Segment {
                @Id
                @Column(name = Names.KEY)
                private Long id;
            }
            """.trimIndent()
        )

        val compact = compactRecordOf("com.example.quoted.Segment")
        val detailed = detailedRecordOf("com.example.quoted.Segment")

        assertEquals("Segment", compact["tableName"].asText())
        assertEquals(true, compact["tableQuoted"].booleanValue())
        assertEquals("SegmentId", field(detailed["fields"], "id")["column"].asText())
        assertEquals(true, field(detailed["fields"], "id")["columnQuoted"].booleanValue())
    }

    fun testAnUnquotedEntityKeepsExactlyItsKeys() = runBlocking<Unit> {
        myFixture.copyDirectoryToProject("springBootApp", "")

        val compact = compactRecordOf(ORDER_ENTITY)
        val detailed = detailedRecordOf(ORDER_ENTITY)
        val demo = field(detailed["fields"], "demo")

        assertEquals("orders", compact["tableName"].asText())
        assertEquals(setOf("name", "className", "tableName", "filePath", "line"), keysOf(compact))
        assertEquals("demo_id", demo["joinColumn"].asText())
        assertEquals(
            setOf("name", "type", "column", "primaryKey", "nullable", "relationship", "joinColumn", "mappedBy"),
            keysOf(demo)
        )
        assertEquals(keysOf(demo), keysOf(field(detailed["fields"], "reference")))
    }

    fun testAMapsIdAssociationIsPartOfThePrimaryKey() = runBlocking<Unit> {
        addOwnerEntity()
        myFixture.addFileToProject(
            "com/example/fk/Profile.java", """
            package com.example.fk;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.MapsId;
            import jakarta.persistence.OneToOne;

            @Entity
            public class Profile {
                @Id
                private Long id;

                @MapsId
                @OneToOne
                @JoinColumn(name = "user_id")
                private Owner user;

                private String bio;
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.fk.Profile")

        assertEquals(true, field(fields, "user")["primaryKey"].booleanValue())
        assertEquals("A @MapsId join column belongs to the primary key", false, field(fields, "user")["nullable"].booleanValue())
        assertEquals("user_id", field(fields, "user")["joinColumn"].asText())
        assertEquals(true, field(fields, "bio")["nullable"].booleanValue())
    }

    fun testAMandatoryForeignKeyIsNotNullable() = runBlocking<Unit> {
        addOwnerEntity()
        myFixture.addFileToProject(
            "com/example/fk/Task.java", """
            package com.example.fk;

            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.JoinColumn;
            import jakarta.persistence.ManyToOne;

            @Entity
            public class Task {
                @Id
                private Long id;

                @ManyToOne
                @JoinColumn(name = "owner_id", nullable = false)
                private Owner owner;

                @ManyToOne(optional = false)
                private Owner boss;

                @ManyToOne
                private Owner peer;

                @ManyToOne
                @JoinColumn(name = "reviewer_id")
                private Owner reviewer;

                @ManyToOne
                @JoinColumn(name = "auditor_id")
                @NotNull
                private Owner auditor;

                @Column(name = "code")
                @NotNull
                private String code;
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.fk.Task")

        assertEquals("@JoinColumn(nullable = false)", false, field(fields, "owner")["nullable"].booleanValue())
        assertEquals("@ManyToOne(optional = false)", false, field(fields, "boss")["nullable"].booleanValue())
        assertEquals("A plain to-one is optional", true, field(fields, "peer")["nullable"].booleanValue())
        assertEquals("A @JoinColumn without nullable keeps the default", true, field(fields, "reviewer")["nullable"].booleanValue())
        assertEquals("@NotNull is not masked by a @JoinColumn that does not say nullable", false, field(fields, "auditor")["nullable"].booleanValue())
        assertEquals("@NotNull is not masked by a @Column that does not say nullable", false, field(fields, "code")["nullable"].booleanValue())
    }

    fun testAKotlinMandatoryForeignKeyIsNotNullable() = runBlocking<Unit> {
        addOwnerEntity()
        myFixture.addFileToProject(
            "com/example/fk/Ticket.kt", """
            package com.example.fk

            import jakarta.persistence.Entity
            import jakarta.persistence.Id
            import jakarta.persistence.ManyToOne

            @Entity
            class Ticket {
                @Id
                var id: Long? = null

                @ManyToOne(optional = false)
                var owner: Owner? = null

                @ManyToOne
                var watcher: Owner? = null
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.fk.Ticket")

        assertEquals(false, field(fields, "owner")["nullable"].booleanValue())
        assertEquals(true, field(fields, "watcher")["nullable"].booleanValue())
    }

    fun testABracketQuotedTableNameIsReportedWithoutTheBrackets() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/quoted/Order.java", """
            package com.example.quoted;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.Table;

            @Entity
            @Table(name = "[Order]")
            public class Order {
                @Id
                private Long id;
            }
            """.trimIndent()
        )

        val compact = compactRecordOf("com.example.quoted.Order")

        assertEquals("Order", compact["tableName"].asText())
        assertEquals(true, compact["tableQuoted"].booleanValue())
    }

    fun testAnIndexOverAQuotedColumnListsTheColumnWithoutItsQuotes() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/quoted/Coupon.java", """
            package com.example.quoted;

            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.Index;
            import jakarta.persistence.Table;

            @Entity
            @Table(name = "coupons", indexes = @Index(name = "ix_coupon_code", columnList = "`Code`, issued_at"))
            public class Coupon {
                @Id
                private Long id;

                @Column(name = "`Code`")
                private String code;

                @Column(name = "issued_at")
                private String issuedAt;
            }
            """.trimIndent()
        )

        val detailed = detailedRecordOf("com.example.quoted.Coupon")

        assertEquals("Code", field(detailed["fields"], "code")["column"].asText())
        assertEquals(listOf("Code", "issued_at"), detailed["indexes"][0]["columns"].map { it.asText() })
    }

    fun testAnEntityNameIsTheTableNameWhenNoTableIsDeclared() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/named/GraphNode.java", """
            package com.example.named;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;

            @Entity(name = "Node")
            public class GraphNode {
                @Id
                private Long id;
            }
            """.trimIndent()
        )

        val compact = compactRecordOf("com.example.named.GraphNode")

        assertEquals("Node", compact["tableName"].asText())
        assertFalse("An entity name is not a quoted identifier", compact.has("tableQuoted"))
    }

    private fun addOwnerEntity() {
        myFixture.addFileToProject(
            "com/example/fk/NotNull.java", """
            package com.example.fk;

            public @interface NotNull {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/fk/Owner.java", """
            package com.example.fk;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;

            @Entity
            public class Owner {
                @Id
                private Long id;
            }
            """.trimIndent()
        )
    }

    private suspend fun fieldsOf(className: String): JsonNode = detailedRecordOf(className)["fields"]

    private suspend fun compactRecordOf(className: String): JsonNode = singleRecordOf(className, includeDetails = false)

    private suspend fun detailedRecordOf(className: String): JsonNode = singleRecordOf(className, includeDetails = true)

    private suspend fun singleRecordOf(className: String, includeDetails: Boolean): JsonNode {
        val page = parse(
            toolset.getSpringDataEntities(
                projectPath = projectPath(),
                className = className,
                includeDetails = includeDetails
            )
        )
        assertEquals("Precondition: the entity must be found, got $page", 1, page["totalCount"].asInt())
        return page["entities"][0]
    }

    private fun keysOf(node: JsonNode): Set<String> = node.fieldNames().asSequence().toSet()

    private fun field(fields: JsonNode, name: String): JsonNode = fields.single { it["name"].asText() == name }

    private companion object {
        const val DEMO_ENTITY = "com.example.app.entity.DemoEntity"
        const val ORDER_ENTITY = "com.example.app.entity.OrderEntity"
        const val WAREHOUSE_ENTITY =
            "com.example.app.integration.persistence.warehouse.entity.WarehouseInventoryAdjustmentEntity"
    }
}
