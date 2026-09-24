/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.entities

import com.explyt.spring.ai.mcp.EntityFieldJson
import com.explyt.spring.ai.mcp.EntityIndexJson
import com.explyt.spring.core.service.beans.BeanSnapshotIdentity
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker

/** Schema of one entity: everything a compact listing deliberately leaves out. */
data class EntitySchema(
    val fields: List<EntityFieldJson>,
    val indexes: List<EntityIndexJson>
)

/**
 * One entity's identity, plus the means to read its schema when a caller actually asks for it.
 *
 * [readSchema] is a function rather than a computed value because expanding fields, relationships and indexes
 * is the expensive half of this tool: a page of twenty names must not pay for the schema of every entity in
 * the project.
 */
class EntityRecord(
    val name: String,
    val className: String,
    val tableName: String,
    val filePath: String?,
    val line: Int?,
    val readSchema: () -> EntitySchema
)

/**
 * Turns entities into the two fixed JSON projections the tool promises, and versions the answer they belong to.
 *
 * Which fields a projection carries is decided here rather than per record, so a client can parse the shape
 * without inspecting it: a compact record never carries a schema, and a detailed one always does - including
 * the empty index list of an entity that genuinely declares none. Emitting `indexes` only when non-empty would
 * report "this entity has no index" and "the schema was not read" as the same thing.
 */
class EntityInventory(private val mapper: ObjectMapper = ObjectMapper()) {

    /**
     * The order every page is cut from.
     *
     * Fixed before anything is projected: a continuation that re-derived its order from a re-read model could
     * repeat an entity on one page and skip another.
     */
    fun ordered(records: List<EntityRecord>): List<EntityRecord> = records.sortedBy { it.className }

    fun compact(record: EntityRecord): ObjectNode {
        val node = mapper.createObjectNode()
        node.put("name", record.name)
        node.put("className", record.className)
        node.put("tableName", record.tableName)
        node.put("filePath", record.filePath)
        node.put("line", record.line)
        return node
    }

    fun details(record: EntityRecord): ObjectNode {
        val node = compact(record)
        val schema = record.readSchema()
        node.set<ArrayNode>("fields", mapper.valueToTree(schema.fields))
        node.set<ArrayNode>("indexes", mapper.valueToTree(schema.indexes))
        return node
    }

    companion object {

        /**
         * Opaque version of one answer: the PSI state it was read from, plus the question that was asked.
         *
         * The stamp is deliberately the whole project's modification count rather than something narrower -
         * an edit to any entity changes which columns a later page would describe, and a continuation that
         * missed it would serve a page of a result set that no longer exists.
         *
         * Paging parameters are stripped rather than trusted to be absent: binding `limit` would turn every
         * page-size change into `RESULT_CHANGED` for an answer that did not change at all.
         */
        fun revision(project: Project, normalizedQuery: Map<String, String?>): String {
            val stamp = PsiModificationTracker.getInstance(project).modificationCount.toString()
            return BeanSnapshotIdentity.hash(
                listOf(stamp) + normalizedQuery.entries
                    .filterNot { it.key in PAGING_KEYS }
                    .sortedBy { it.key }
                    .flatMap { listOf(it.key, it.value ?: NULL_MARKER) }
            )
        }

        private val PAGING_KEYS = setOf("offset", "limit", "maxChars", "expectedRevision")

        /** Distinguishes an absent key from one explicitly set to the string "null". */
        private const val NULL_MARKER = "\u0000null"
    }
}
