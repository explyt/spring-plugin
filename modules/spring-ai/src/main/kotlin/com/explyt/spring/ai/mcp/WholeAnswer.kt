/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.beans.BeanSnapshotIdentity
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.progress.ProgressManager

/**
 * Every record of one paged answer, projected once, and the [digest] that fingerprints it.
 *
 * A model tracker alone cannot version an answer read from PSI on every call: a method body, a field initializer
 * or a field type moves no structural tracker, yet it changes a traced chain, a contract's service calls or a
 * response schema. The digest covers exactly what the caller would receive - the envelope, any side array and
 * every record in order - so an equal answer keeps its revision across indexing or dropped caches, and a
 * different one never shares it.
 */
class WholeAnswer private constructor(val items: List<ObjectNode>, val digest: String) {

    val totalCount: Int get() = items.size

    fun itemAt(index: Int): ObjectNode = items[index]

    companion object {

        fun project(
            envelope: JsonNode,
            totalCount: Int,
            itemAt: (Int) -> ObjectNode,
            context: List<JsonNode> = emptyList(),
        ): WholeAnswer {
            val items = List(totalCount) { index ->
                ProgressManager.checkCanceled()
                itemAt(index)
            }
            val digest = BeanSnapshotIdentity.hash((listOf(envelope) + context + items).map(mapper::writeValueAsString))
            return WholeAnswer(items, digest)
        }

        private val mapper = ObjectMapper()
    }
}
