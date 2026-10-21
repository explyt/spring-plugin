/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.ai.mcp.BoundedPageWriter
import com.explyt.spring.ai.mcp.PageRequest
import com.explyt.spring.core.service.beans.BeanQueryProblem
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * One query's answer, ready to be paged but not yet serialized.
 *
 * [candidateAt] projects a single record on demand instead of handing over a finished list: a page of five must
 * not pay for the annotations of every bean in the context. It is a plain synchronous function, not a cursor -
 * when it captures PSI, the whole content is built and consumed inside one read action and only the finished
 * JSON string leaves it.
 */
data class BeanResponseContent(
    val mode: String,
    val model: ObjectNode,
    val outcome: String,
    val matchCompleteness: String,
    val unresolvedCount: Int,
    val totalCount: Int,
    val candidateAt: (Int) -> ObjectNode,
    val injection: ObjectNode? = null,
    val inactiveCount: Int = 0,
    val inactiveAt: (Int) -> ObjectNode = { throw IndexOutOfBoundsException(it) }
)

typealias BeanPageRequest = PageRequest

/**
 * Describes a bean answer; [BoundedPageWriter] decides how much of it fits.
 *
 * Only the fields that make an answer a *bean* answer live here - what paging and budgeting mean is the same
 * question for every tool, and is answered once in the writer this delegates to.
 */
class BoundedBeanResponseWriter {

    fun write(content: BeanResponseContent, revision: String, page: BeanPageRequest): String {
        val first by lazy { content.candidateAt(page.offset) }
        val candidateAt = { index: Int -> if (index == page.offset) first else content.candidateAt(index) }
        val envelope = envelope(content)
        val sampled = when {
            content.inactiveCount == 0 || page.offset != 0 -> envelope
            content.totalCount == 0 -> envelope.withInactiveSample(content, revision, page, firstCandidate = null)
            else -> envelope.withInactiveSample(content, revision, page, first)
        }
        return writer.write(sampled, FIELD_CANDIDATES, content.totalCount, candidateAt, revision, page)
    }

    fun writeError(problem: BeanQueryProblem, maxChars: Int): String =
        writer.writeError(problem.code, problem.message, problem.choices, maxChars)

    private fun envelope(content: BeanResponseContent): ObjectNode {
        val envelope = mapper.createObjectNode()
        envelope.put("mode", content.mode)
        envelope.set<ObjectNode>("model", content.model)
        envelope.put("outcome", content.outcome)
        envelope.put("matchCompleteness", content.matchCompleteness)
        envelope.put("unresolvedCount", content.unresolvedCount)
        content.injection?.let { envelope.set<ObjectNode>("injection", it) }
        if (content.inactiveCount > 0) envelope.put(FIELD_INACTIVE_COUNT, content.inactiveCount)
        return envelope
    }

    private fun ObjectNode.withInactiveSample(
        content: BeanResponseContent,
        revision: String,
        page: BeanPageRequest,
        firstCandidate: ObjectNode?
    ): ObjectNode {
        val sample = List(minOf(content.inactiveCount, MAX_INACTIVE_SAMPLE), content.inactiveAt)
        val fitted = writer.fitSample(
            this, FIELD_CANDIDATES, FIELD_INACTIVE_CANDIDATES, sample, FIELD_INACTIVE_TRUNCATED,
            content.totalCount, revision, page, firstCandidate, maxSampleChars = page.maxChars / 2
        )
        val served = fitted[FIELD_INACTIVE_CANDIDATES].size()
        if (served == 0) fitted.remove(FIELD_INACTIVE_CANDIDATES)
        if (served < content.inactiveCount) fitted.put(FIELD_INACTIVE_TRUNCATED, true)
        return fitted
    }

    private val writer = BoundedPageWriter()

    private val mapper = ObjectMapper()

    companion object {
        const val INVALID_ARGUMENT = BoundedPageWriter.INVALID_ARGUMENT
        const val RESULT_CHANGED = BoundedPageWriter.RESULT_CHANGED
        const val RESPONSE_TOO_LARGE = BoundedPageWriter.RESPONSE_TOO_LARGE

        const val MAX_CHARS = BoundedPageWriter.MAX_CHARS
        const val FALLBACK_BUDGET = BoundedPageWriter.FALLBACK_BUDGET

        private const val FIELD_CANDIDATES = "candidates"
        private const val FIELD_INACTIVE_COUNT = "inactiveCount"
        private const val FIELD_INACTIVE_CANDIDATES = "inactiveCandidates"
        private const val FIELD_INACTIVE_TRUNCATED = "inactiveCandidatesTruncated"
        private const val MAX_INACTIVE_SAMPLE = 3
    }
}
