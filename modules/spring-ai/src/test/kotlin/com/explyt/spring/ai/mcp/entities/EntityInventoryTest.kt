/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.entities

import com.explyt.spring.ai.mcp.EntityFieldJson
import com.explyt.spring.ai.mcp.EntityIndexJson
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary

/**
 * The projection is a contract a client parses, and a page only continues the previous one because the order
 * was fixed before anything was projected.
 */
class EntityInventoryTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.jakarta_persistence_3_1_0)

    private val inventory = EntityInventory()

    /** The compact listing exists to be small: a client that sees `fields` cannot tell "none" from "not read". */
    fun testCompactProjectionOmitsTheSchemaRatherThanEmptyingIt() {
        val record = record("Order", "com.example.Order", schema = schemaWithFields())

        assertTrue(
            "Precondition: the record must carry a schema, or omitting it proves nothing",
            record.readSchema().fields.isNotEmpty()
        )

        val node = inventory.compact(record)

        assertEquals("Order", node["name"].asText())
        assertEquals("com.example.Order", node["className"].asText())
        assertEquals("orders", node["tableName"].asText())
        assertEquals("src/main/java/com/example/Order.java", node["filePath"].asText())
        assertEquals(12, node["line"].asInt())
        assertFalse("A compact record must not carry fields", node.has("fields"))
        assertFalse("A compact record must not carry indexes", node.has("indexes"))
    }

    /** An entity that genuinely declares no index says so; that is an answer, not a missing one. */
    fun testDetailsProjectionCarriesTheSchemaIncludingKnownEmptyIndexes() {
        val node = inventory.details(record("Order", "com.example.Order", schema = schemaWithFields()))

        assertEquals("Order", node["name"].asText())
        assertEquals(listOf("id", "reference"), node["fields"].map { it["name"].asText() })
        assertEquals("reference", node["fields"][1]["column"].asText())
        assertTrue("A known-empty index list is an answer and must survive", node.has("indexes"))
        assertEquals(0, node["indexes"].size())
    }

    fun testDetailsProjectionReportsDeclaredIndexes() {
        val schema = EntitySchema(
            fields = emptyList(),
            indexes = listOf(EntityIndexJson(name = "ix_placed_at", columns = listOf("placed_at"), unique = true))
        )

        val node = inventory.details(record("Order", "com.example.Order", schema = schema))

        assertEquals(1, node["indexes"].size())
        assertEquals("ix_placed_at", node["indexes"][0]["name"].asText())
        assertEquals(listOf("placed_at"), node["indexes"][0]["columns"].map { it.asText() })
        assertTrue(node["indexes"][0]["unique"].asBoolean())
    }

    /** An entity with no physical declaration keeps its row; a path and a line are simply unknown. */
    fun testUnknownSourcePositionIsReportedAsNullRatherThanDropped() {
        val node = inventory.compact(
            EntityRecord("Ghost", "com.example.Ghost", "ghost", null, null) { EntitySchema(emptyList(), emptyList()) }
        )

        assertEquals("com.example.Ghost", node["className"].asText())
        assertTrue("An unknown path is null, not absent", node["filePath"].isNull)
        assertTrue("An unknown line is null, not absent", node["line"].isNull)
    }

    /** Reordering the input must not reorder the answer, or page two would repeat or skip an entity. */
    fun testOrderDoesNotDependOnInputOrder() {
        val records = listOf(
            record("Shipment", "com.example.Shipment"),
            record("Audit", "com.example.reporting.Audit"),
            record("Order", "com.example.Order")
        )

        val forward = inventory.ordered(records).map { it.className }
        val reversed = inventory.ordered(records.reversed()).map { it.className }

        assertEquals(
            listOf("com.example.Order", "com.example.Shipment", "com.example.reporting.Audit"),
            forward
        )
        assertEquals(forward, reversed)
    }

    /**
     * A page of one must read one schema. An eagerly built list would pay the expensive half of this tool for
     * every entity in the project to answer a question about a single one.
     */
    fun testProjectingOnePageReadsOnlyThatPagesSchemas() {
        var reads = 0
        val records = (1..5).map { index ->
            record("Entity$index", "com.example.Entity$index", schema = schemaWithFields(), onRead = { reads++ })
        }

        val ordered = inventory.ordered(records)
        inventory.details(ordered[0])

        assertEquals("Only the projected record's schema may be read", 1, reads)
    }

    /** A continuation describes one question; a different filter is a different question. */
    fun testRevisionChangesWithTheQuery() {
        val base = EntityInventory.revision(project, query(packageFilter = "com.example.app"))

        assertEquals("The same question yields the same revision", base, EntityInventory.revision(project, query(packageFilter = "com.example.app")))
        assertTrue(
            "A different package filter is a different question",
            base != EntityInventory.revision(project, query(packageFilter = "com.example.reporting"))
        )
        assertTrue(
            "A different entity is a different question",
            base != EntityInventory.revision(project, query(packageFilter = "com.example.app", className = "com.example.app.Order"))
        )
        assertTrue(
            "Details and inventory are different answers",
            base != EntityInventory.revision(project, query(packageFilter = "com.example.app", includeDetails = true))
        )
    }

    /**
     * `limit` and `maxChars` may change between pages of the same answer; binding them would reject a
     * continuation that describes the very same result.
     */
    fun testRevisionIgnoresPagingParameters() {
        val base = EntityInventory.revision(project, query(packageFilter = "com.example.app"))
        val paged = EntityInventory.revision(
            project,
            query(packageFilter = "com.example.app") + mapOf("limit" to "50", "maxChars" to "4000", "offset" to "20")
        )

        assertTrue(
            "Precondition: the base revision must be a real value, or equality proves nothing",
            base.isNotEmpty()
        )
        assertEquals("Paging parameters are not part of the question", base, paged)
    }

    private fun query(
        packageFilter: String? = null,
        className: String? = null,
        includeDetails: Boolean = false
    ): Map<String, String?> = mapOf(
        "packageFilter" to packageFilter,
        "className" to className,
        "includeDetails" to includeDetails.toString()
    )

    private fun schemaWithFields() = EntitySchema(
        fields = listOf(
            EntityFieldJson("id", "java.lang.Long", "id", primaryKey = true, nullable = false, null, null, null),
            EntityFieldJson(
                "reference", "java.lang.String", "reference", primaryKey = false, nullable = false, null, null, null
            )
        ),
        indexes = emptyList()
    )

    private fun record(
        name: String,
        className: String,
        schema: EntitySchema = EntitySchema(emptyList(), emptyList()),
        onRead: () -> Unit = {}
    ) = EntityRecord(
        name = name,
        className = className,
        tableName = "orders",
        filePath = "src/main/java/com/example/Order.java",
        line = 12,
        readSchema = {
            onRead()
            schema
        }
    )
}
