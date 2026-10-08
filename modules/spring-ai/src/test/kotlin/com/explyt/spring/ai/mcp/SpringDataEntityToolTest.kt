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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.GlobalSearchScope
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
        TestLibrary.javax_persistence_2_2,
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

    fun testAnImplicitPrimitiveBooleanColumnIsNotNullable() = runBlocking<Unit> {
        val active = field(fieldsOf(addSwitchEntity()), "active")

        assertEquals("Precondition: the property must be primitive", "boolean", active["type"].asText())
        assertEquals("A primitive without @Column maps to a NOT NULL column", false, active["nullable"].booleanValue())
    }

    fun testAnImplicitPrimitiveLongColumnIsNotNullable() = runBlocking<Unit> {
        val version = field(fieldsOf(addSwitchEntity()), "version")

        assertEquals("Precondition: the property must be primitive", "long", version["type"].asText())
        assertEquals("A primitive without @Column maps to a NOT NULL column", false, version["nullable"].booleanValue())
    }

    fun testAnImplicitWrapperColumnStaysNullable() = runBlocking<Unit> {
        val flag = field(fieldsOf(addSwitchEntity()), "flag")

        assertEquals("Precondition: the property must be boxed", "java.lang.Boolean", flag["type"].asText())
        assertEquals(true, flag["nullable"].booleanValue())
    }

    fun testAnExplicitColumnOnAPrimitiveKeepsItsDefaultNullable() = runBlocking<Unit> {
        val deleted = field(fieldsOf(addSwitchEntity()), "deleted")

        assertEquals("Precondition: the property must be primitive", "boolean", deleted["type"].asText())
        assertEquals("is_deleted", deleted["column"].asText())
        assertEquals("An explicit @Column ignores primitiveness", true, deleted["nullable"].booleanValue())
    }

    fun testAnExplicitNotNullableColumnOnAWrapperIsNotNullable() = runBlocking<Unit> {
        val archived = field(fieldsOf(addSwitchEntity()), "archived")

        assertEquals("Precondition: the property must be boxed", "java.lang.Boolean", archived["type"].asText())
        assertEquals(false, archived["nullable"].booleanValue())
    }

    fun testAMandatoryBasicColumnIsNotNullable() = runBlocking<Unit> {
        val code = field(fieldsOf(addSwitchEntity()), "code")

        assertEquals("Precondition: the property must be a reference", "java.lang.String", code["type"].asText())
        assertEquals("@Basic(optional = false) maps to a NOT NULL column", false, code["nullable"].booleanValue())
    }

    fun testAnOptionalBasicPrimitiveColumnIsNullable() = runBlocking<Unit> {
        val priority = field(fieldsOf(addSwitchEntity()), "priority")

        assertEquals("Precondition: the property must be primitive", "int", priority["type"].asText())
        assertEquals("@Basic(optional = true) overrides primitiveness", true, priority["nullable"].booleanValue())
    }

    fun testABareBasicPrimitiveColumnIsNullable() = runBlocking<Unit> {
        val rank = field(fieldsOf(addSwitchEntity()), "rank")

        assertEquals("Precondition: the property must be primitive", "int", rank["type"].asText())
        assertEquals("@Basic defaults to optional = true", true, rank["nullable"].booleanValue())
    }

    fun testAnImplicitPrimitiveInheritedFromAMappedSuperclassIsNotNullable() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/primitive/Versioned.java", """
            package com.example.primitive;

            import jakarta.persistence.MappedSuperclass;

            @MappedSuperclass
            public abstract class Versioned {
                private long revision;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/primitive/Document.java", """
            package com.example.primitive;

            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;

            @Entity
            public class Document extends Versioned {
                @Id
                private Long id;
            }
            """.trimIndent()
        )

        val revision = field(fieldsOf("com.example.primitive.Document"), "revision")

        assertEquals("Precondition: the property must be primitive", "long", revision["type"].asText())
        assertEquals("An inherited primitive without @Column maps to a NOT NULL column", false, revision["nullable"].booleanValue())
    }

    fun testKotlinNonNullPrimitivePropertiesAreNotNullableColumns() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/primitive/Toggle.kt", """
            package com.example.primitive

            import jakarta.persistence.Entity
            import jakarta.persistence.Id

            @Entity
            class Toggle(@Id var id: Long? = null) {
                var enabled: Boolean = false
                var counter: Long = 0
                var maybe: Boolean? = null
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.primitive.Toggle")

        assertEquals("Precondition: a non-null Boolean compiles to a primitive", "boolean", field(fields, "enabled")["type"].asText())
        assertEquals("Precondition: a non-null Long compiles to a primitive", "long", field(fields, "counter")["type"].asText())
        assertEquals("Precondition: a nullable Boolean is boxed", "java.lang.Boolean", field(fields, "maybe")["type"].asText())
        assertEquals("A boxed property stays nullable", true, field(fields, "maybe")["nullable"].booleanValue())
        assertEquals("enabled maps to a NOT NULL column", false, field(fields, "enabled")["nullable"].booleanValue())
        assertEquals("counter maps to a NOT NULL column", false, field(fields, "counter")["nullable"].booleanValue())
    }

    fun testAMandatoryBasicOverridesAnExplicitlyNullableColumn() = runBlocking<Unit> {
        val label = field(fieldsOf(addGateEntity()), "label")

        assertEquals("Precondition: the property must be a reference", "java.lang.String", label["type"].asText())
        assertEquals("@Basic(optional = false) forces NOT NULL over @Column(nullable = true)", false, label["nullable"].booleanValue())
    }

    fun testAMandatoryBasicPrimitiveWithoutAColumnIsNotNullable() = runBlocking<Unit> {
        val weight = field(fieldsOf(addGateEntity()), "weight")

        assertEquals("Precondition: the property must be primitive", "int", weight["type"].asText())
        assertEquals(false, weight["nullable"].booleanValue())
    }

    fun testANotNullPrimitiveWithAnExplicitColumnIsNotNullable() = runBlocking<Unit> {
        val open = field(fieldsOf(addGateEntity()), "open")

        assertEquals("Precondition: the property must be primitive", "boolean", open["type"].asText())
        assertEquals("gate_open", open["column"].asText())
        assertEquals("@NotNull is not masked by a @Column that does not say nullable", false, open["nullable"].booleanValue())
    }

    fun testJavaxBasicAnnotationsFollowTheSameRules() = runBlocking<Unit> {
        myFixture.addFileToProject(
            "com/example/legacy/Lever.java", """
            package com.example.legacy;

            import javax.persistence.Basic;
            import javax.persistence.Entity;
            import javax.persistence.Id;

            @Entity
            public class Lever {
                @Id
                private Long id;

                @Basic(optional = false)
                private String name;

                @Basic
                private int position;

                private boolean locked;
            }
            """.trimIndent()
        )

        val fields = fieldsOf("com.example.legacy.Lever")

        assertEquals("Precondition: the property must be a reference", "java.lang.String", field(fields, "name")["type"].asText())
        assertEquals("Precondition: the property must be primitive", "int", field(fields, "position")["type"].asText())
        assertEquals("Precondition: the property must be primitive", "boolean", field(fields, "locked")["type"].asText())
        assertEquals("javax @Basic(optional = false)", false, field(fields, "name")["nullable"].booleanValue())
        assertEquals("A bare javax @Basic keeps a primitive nullable", true, field(fields, "position")["nullable"].booleanValue())
        assertEquals("An implicit primitive column under javax", false, field(fields, "locked")["nullable"].booleanValue())
    }

    fun testJavaSingleTableNotNullSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = false, name = "code")
    }

    fun testKotlinSingleTableNotNullSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = true, name = "code")
    }

    fun testJavaSingleTableMandatoryBasicSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = false, name = "name")
    }

    fun testKotlinSingleTableMandatoryBasicSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = true, name = "name")
    }

    fun testJavaSingleTableImplicitPrimitiveSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = false, name = "level")
    }

    fun testKotlinSingleTableImplicitPrimitiveSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = true, name = "level")
    }

    fun testJavaDefaultSingleTableSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = false, name = "code", strategy = null)
    }

    fun testKotlinDefaultSingleTableSubclassColumnIsNullable() = runBlocking<Unit> {
        assertForcedSubclassColumn(kotlin = true, name = "code", strategy = null)
    }

    fun testJavaSingleTableExplicitNotNullSubclassColumnKeepsItsConstraint() = runBlocking<Unit> {
        assertExplicitSubclassColumn(kotlin = false)
    }

    fun testKotlinSingleTableExplicitNotNullSubclassColumnKeepsItsConstraint() = runBlocking<Unit> {
        assertExplicitSubclassColumn(kotlin = true)
    }

    fun testJavaSingleTableRootColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertRootColumns(kotlin = false)
    }

    fun testKotlinSingleTableRootColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertRootColumns(kotlin = true)
    }

    fun testJavaJoinedSubclassColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertOtherStrategyColumns(kotlin = false, strategy = "JOINED")
    }

    fun testKotlinJoinedSubclassColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertOtherStrategyColumns(kotlin = true, strategy = "JOINED")
    }

    fun testJavaTablePerClassSubclassColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertOtherStrategyColumns(kotlin = false, strategy = "TABLE_PER_CLASS")
    }

    fun testKotlinTablePerClassSubclassColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertOtherStrategyColumns(kotlin = true, strategy = "TABLE_PER_CLASS")
    }

    fun testJavaMappedSuperclassColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertMappedSuperclassColumns(kotlin = false)
    }

    fun testKotlinMappedSuperclassColumnsKeepTheirConstraints() = runBlocking<Unit> {
        assertMappedSuperclassColumns(kotlin = true)
    }

    private suspend fun assertForcedSubclassColumn(kotlin: Boolean, name: String, strategy: String? = "SINGLE_TABLE") {
        val subclass = addColumnHierarchy(kotlin, strategy)
        val property = subclass.findFieldByName(name, false) ?: error("Missing subclass property $name")
        assertEquals("Precondition: the property belongs to the subclass", subclass, property.containingClass)
        when (name) {
            "code" -> assertNotNull("Precondition: @NotNull is present", property.getAnnotation("explyt.inheritance.NotNull"))
            "name" -> assertEquals(
                "Precondition: @Basic explicitly forbids null", "false",
                property.getAnnotation("jakarta.persistence.Basic")?.findDeclaredAttributeValue("optional")?.text
            )
            "level" -> {
                assertEquals("Precondition: the property is primitive", "int", property.type.canonicalText)
                assertNull("Precondition: the primitive has no explicit column", property.getAnnotation("jakarta.persistence.Column"))
            }
        }
        val fields = fieldsOf(subclass.qualifiedName!!)
        assertColumnWithoutInheritanceReason(field(fields, "explicit"), nullable = false)
        assertColumnWithoutInheritanceReason(field(fields, "rootCode"), nullable = false)
        assertColumnWithoutInheritanceReason(field(fields, "id"), nullable = false)
        assertColumnWithoutInheritanceReason(field(fields, "unconstrained"), nullable = true)
        val column = field(fields, name)
        assertTrue("The column must carry a Boolean nullable key", column.has("nullable") && column["nullable"].isBoolean)
        assertEquals("SINGLE_TABLE subclass $name must allow other subclasses' rows", true, column["nullable"].booleanValue())
        assertTrue("A forced nullable column must explain its inheritance rule", column.has("nullableReason"))
        assertEquals("SINGLE_TABLE_SUBCLASS", column["nullableReason"].asText())
    }

    private suspend fun assertExplicitSubclassColumn(kotlin: Boolean) {
        val subclass = addColumnHierarchy(kotlin)
        val property = subclass.findFieldByName("explicit", false) ?: error("Missing explicit column")
        assertEquals(
            "Precondition: @Column explicitly forbids null", "false",
            property.getAnnotation("jakarta.persistence.Column")?.findDeclaredAttributeValue("nullable")?.text
        )
        assertColumnWithoutInheritanceReason(field(fieldsOf(subclass.qualifiedName!!), "explicit"), nullable = false)
    }

    private suspend fun assertRootColumns(kotlin: Boolean) {
        val subclass = addColumnHierarchy(kotlin)
        val root = subclass.superClass ?: error("Missing root entity")
        val rootFields = fieldsOf(root.qualifiedName!!)
        val inheritedFields = fieldsOf(subclass.qualifiedName!!)
        for (name in listOf("id", "rootCode", "rootName", "rootLevel")) {
            assertEquals("Precondition: $name is declared on the root", root, subclass.findFieldByName(name, true)?.containingClass)
            assertColumnWithoutInheritanceReason(field(rootFields, name), nullable = false)
            assertColumnWithoutInheritanceReason(field(inheritedFields, name), nullable = false)
        }
        assertColumnWithoutInheritanceReason(field(rootFields, "rootOptional"), nullable = true)
        assertColumnWithoutInheritanceReason(field(inheritedFields, "rootOptional"), nullable = true)
    }

    private suspend fun assertOtherStrategyColumns(kotlin: Boolean, strategy: String) {
        val subclass = addColumnHierarchy(kotlin, strategy)
        val fields = fieldsOf(subclass.qualifiedName!!)
        assertEquals("Precondition: subclass primitive is present", "int", field(fields, "level")["type"].asText())
        for (name in listOf("code", "name", "level", "explicit", "id", "rootCode", "rootName", "rootLevel")) {
            assertColumnWithoutInheritanceReason(field(fields, name), nullable = false)
        }
        for (name in listOf("unconstrained", "rootOptional")) {
            assertColumnWithoutInheritanceReason(field(fields, name), nullable = true)
        }
    }

    private suspend fun assertMappedSuperclassColumns(kotlin: Boolean) {
        val entity = addColumnHierarchy(kotlin, strategy = null, mappedSuperclass = true)
        val fields = fieldsOf(entity.qualifiedName!!)
        for (name in listOf("id", "rootCode", "rootName", "rootLevel")) {
            assertEquals("Precondition: $name belongs to the mapped superclass", entity.superClass, entity.findFieldByName(name, true)?.containingClass)
            assertColumnWithoutInheritanceReason(field(fields, name), nullable = false)
        }
        assertColumnWithoutInheritanceReason(field(fields, "rootOptional"), nullable = true)
    }

    private fun assertColumnWithoutInheritanceReason(column: JsonNode, nullable: Boolean) {
        assertTrue("The column must carry a Boolean nullable key: $column", column.has("nullable") && column["nullable"].isBoolean)
        assertEquals("Column nullability stays unchanged: $column", nullable, column["nullable"].booleanValue())
        assertFalse("A column unaffected by forced nullability must omit nullableReason: $column", column.has("nullableReason"))
    }

    private fun addColumnHierarchy(kotlin: Boolean, strategy: String? = "SINGLE_TABLE", mappedSuperclass: Boolean = false): PsiClass {
        myFixture.addFileToProject(
            "explyt/inheritance/NotNull.java", """
            package explyt.inheritance;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            @Target(ElementType.FIELD)
            public @interface NotNull {}
            """.trimIndent()
        )
        val parentAnnotation = if (mappedSuperclass) "@MappedSuperclass" else "@Entity"
        val inheritance = strategy?.let { "@Inheritance(strategy = InheritanceType.$it)" }.orEmpty()
        if (kotlin) {
            myFixture.addFileToProject(
                "explyt/inheritance/Root.kt", """
                package explyt.inheritance
                import jakarta.persistence.*
                $parentAnnotation
                $inheritance
                open class Root {
                    @field:Id var id: Long? = null
                    @field:NotNull var rootCode: String? = null
                    @field:Basic(optional = false) var rootName: String? = null
                    var rootLevel: Int = 0
                    var rootOptional: String? = null
                }
                """.trimIndent()
            )
            myFixture.addFileToProject(
                "explyt/inheritance/Child.kt", """
                package explyt.inheritance
                import jakarta.persistence.*
                @Entity
                class Child : Root() {
                    @field:NotNull var code: String? = null
                    @field:Basic(optional = false) var name: String? = null
                    var level: Int = 0
                    @field:Column(nullable = false) var explicit: String? = null
                    var unconstrained: String? = null
                }
                """.trimIndent()
            )
        } else {
            myFixture.addFileToProject(
                "explyt/inheritance/Root.java", """
                package explyt.inheritance;
                import jakarta.persistence.*;
                $parentAnnotation
                $inheritance
                public class Root {
                    @Id private Long id;
                    @NotNull private String rootCode;
                    @Basic(optional = false) private String rootName;
                    private int rootLevel;
                    private String rootOptional;
                }
                """.trimIndent()
            )
            myFixture.addFileToProject(
                "explyt/inheritance/Child.java", """
                package explyt.inheritance;
                import jakarta.persistence.*;
                @Entity
                public class Child extends Root {
                    @NotNull private String code;
                    @Basic(optional = false) private String name;
                    private int level;
                    @Column(nullable = false) private String explicit;
                    private String unconstrained;
                }
                """.trimIndent()
            )
        }
        val facade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val root = facade.findClass("explyt.inheritance.Root", scope) ?: error("Missing root PSI")
        val child = facade.findClass("explyt.inheritance.Child", scope) ?: error("Missing subclass PSI")
        assertNotNull("Precondition: subclass is an entity", child.getAnnotation("jakarta.persistence.Entity"))
        assertEquals("Precondition: the subclass extends the root", root, child.superClass)
        assertTrue("Precondition: inheritance resolves", child.isInheritor(root, true))
        if (mappedSuperclass) {
            assertNotNull("Precondition: parent is a mapped superclass", root.getAnnotation("jakarta.persistence.MappedSuperclass"))
            assertNull("Precondition: parent is not an entity", root.getAnnotation("jakarta.persistence.Entity"))
        } else {
            assertNotNull("Precondition: root is an entity", root.getAnnotation("jakarta.persistence.Entity"))
            assertNull("Precondition: root has no entity parent", root.superClass?.getAnnotation("jakarta.persistence.Entity"))
        }
        val annotation = root.getAnnotation("jakarta.persistence.Inheritance")
        if (strategy == null) {
            assertNull("Precondition: no inheritance annotation is declared", annotation)
        } else {
            val value = annotation?.findDeclaredAttributeValue("strategy") as? PsiReferenceExpression
            assertEquals("Precondition: declared inheritance strategy resolves", strategy, value?.resolve()?.let { (it as? com.intellij.psi.PsiField)?.name })
        }
        return child
    }

    private fun addGateEntity(): String {
        addOwnerEntity()
        myFixture.addFileToProject(
            "com/example/fk/Gate.java", """
            package com.example.fk;

            import jakarta.persistence.Basic;
            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;

            @Entity
            public class Gate {
                @Id
                private Long id;

                @Basic(optional = false)
                @Column(nullable = true)
                private String label;

                @Basic(optional = false)
                private int weight;

                @Column(name = "gate_open")
                @NotNull
                private boolean open;
            }
            """.trimIndent()
        )
        return "com.example.fk.Gate"
    }

    private fun addSwitchEntity(): String {
        myFixture.addFileToProject(
            "com/example/primitive/Switch.java", """
            package com.example.primitive;

            import jakarta.persistence.Basic;
            import jakarta.persistence.Column;
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;

            @Entity
            public class Switch {
                @Id
                private Long id;

                private boolean active;

                private long version;

                private Boolean flag;

                @Column(name = "is_deleted")
                private boolean deleted;

                @Column(nullable = false)
                private Boolean archived;

                @Basic(optional = false)
                private String code;

                @Basic(optional = true)
                private int priority;

                @Basic
                private int rank;
            }
            """.trimIndent()
        )
        return "com.example.primitive.Switch"
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
                includeDetails = includeDetails,
                maxChars = BoundedPageWriter.MAX_CHARS
            )
        )
        val status = page.path("status").asText("missing")
        val minimumRequiredChars = Regex("at least (\\d+)")
            .find(page.path("error").path("message").asText())?.groupValues?.get(1) ?: "not reported"
        assertEquals(
            "Precondition: entity response status=$status, minimumRequiredChars=$minimumRequiredChars, got $page",
            "OK", status
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
