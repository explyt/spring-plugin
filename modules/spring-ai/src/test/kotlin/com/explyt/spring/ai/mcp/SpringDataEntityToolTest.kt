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

    private companion object {
        const val DEMO_ENTITY = "com.example.app.entity.DemoEntity"
        const val WAREHOUSE_ENTITY =
            "com.example.app.integration.persistence.warehouse.entity.WarehouseInventoryAdjustmentEntity"
    }
}
