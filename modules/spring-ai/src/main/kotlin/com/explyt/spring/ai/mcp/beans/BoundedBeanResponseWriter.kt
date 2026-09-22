/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

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
    val injection: ObjectNode? = null
)

data class BeanPageRequest(
    val offset: Int = 0,
    val limit: Int = 5,
    val maxChars: Int = 1800,
    val expectedRevision: String? = null
)

/**
 * Serializes one page of an answer within a character budget.
 *
 * The budget is measured on the *finished* compact JSON, metadata and continuation included, because that is
 * what the client receives: slicing N candidates and assuming the envelope fits would overshoot exactly when
 * the paths are long. Candidates are appended one at a time and the whole envelope is re-serialized after each,
 * so the first one that does not fit ends the page instead of truncating a value.
 */
class BoundedBeanResponseWriter {

    fun write(content: BeanResponseContent, revision: String, page: BeanPageRequest): String {
        validate(page)?.let { return writeError(it, budgetFor(page)) }
        page.expectedRevision?.let {
            if (it != revision) return writeError(resultChanged(), budgetFor(page))
        }

        val root = envelope(content, revision, page.offset)
        val candidates = root.putArray(FIELD_CANDIDATES)
        if (encodedLength(root) > page.maxChars) {
            return writeError(tooLarge(encodedLength(root)), FALLBACK_BUDGET)
        }

        var added = 0
        while (added < page.limit && page.offset + added < content.totalCount) {
            candidates.add(content.candidateAt(page.offset + added))
            setContinuation(root, content, page.offset + added + 1)
            if (encodedLength(root) > page.maxChars) {
                candidates.remove(candidates.size() - 1)
                break
            }
            added++
        }
        setContinuation(root, content, page.offset + added)

        if (added == 0 && page.offset < content.totalCount) {
            return writeError(tooLarge(minimumFor(content, revision, page)), FALLBACK_BUDGET)
        }
        return mapper.writeValueAsString(root)
    }

    fun writeError(problem: BeanQueryProblem, maxChars: Int): String {
        val root = mapper.createObjectNode()
        root.put(FIELD_STATUS, STATUS_ERROR)
        val error = root.putObject(FIELD_ERROR)
        error.put("code", problem.code)
        error.put("message", problem.message)
        val choices = error.putArray("choices")
        error.put(FIELD_TRUNCATED, false)

        for ((index, choice) in problem.choices.withIndex()) {
            choices.add(mapper.valueToTree<ObjectNode>(choice))
            error.put(FIELD_TRUNCATED, index + 1 < problem.choices.size)
            if (encodedLength(root) > maxChars) {
                choices.remove(choices.size() - 1)
                error.put(FIELD_TRUNCATED, true)
                break
            }
        }
        return mapper.writeValueAsString(root)
    }

    /**
     * The envelope without candidates: everything whose size does not depend on the page.
     *
     * `truncated` and `nextOffset` are written here with their widest shape - a number, not `null` - so that
     * adding the continuation later can only shrink the response, never push an already-measured page over the
     * budget.
     */
    private fun envelope(content: BeanResponseContent, revision: String, offset: Int): ObjectNode {
        val root = mapper.createObjectNode()
        root.put(FIELD_STATUS, STATUS_OK)
        root.put("mode", content.mode)
        root.set<ObjectNode>("model", content.model)
        root.put(FIELD_REVISION, revision)
        root.put("outcome", content.outcome)
        root.put("matchCompleteness", content.matchCompleteness)
        root.put("unresolvedCount", content.unresolvedCount)
        content.injection?.let { root.set<ObjectNode>("injection", it) }
        root.put(FIELD_TOTAL_COUNT, content.totalCount)
        root.put("offset", offset)
        root.put(FIELD_TRUNCATED, true)
        root.put(FIELD_NEXT_OFFSET, content.totalCount)
        return root
    }

    /**
     * Points at the record after the ones served, or terminates the continuation.
     *
     * Applied after every appended candidate so the measured envelope carries the continuation it will be sent
     * with, and once more after the loop: the page that served nothing - an offset past the end - still has to
     * replace the placeholder the envelope was measured with.
     */
    private fun setContinuation(root: ObjectNode, content: BeanResponseContent, served: Int) {
        val remaining = served < content.totalCount
        root.put(FIELD_TRUNCATED, remaining)
        if (remaining) root.put(FIELD_NEXT_OFFSET, served) else root.putNull(FIELD_NEXT_OFFSET)
    }

    /**
     * Size of an envelope holding just the next candidate, so a caller learns the budget that would fetch it.
     *
     * Reported instead of the candidate itself: repeating an element that already overflowed the response is
     * the one thing the budget exists to prevent.
     */
    private fun minimumFor(content: BeanResponseContent, revision: String, page: BeanPageRequest): Int {
        val probe = envelope(content, revision, page.offset)
        probe.putArray(FIELD_CANDIDATES).add(content.candidateAt(page.offset))
        return encodedLength(probe)
    }

    private fun validate(page: BeanPageRequest): BeanQueryProblem? = when {
        page.offset < 0 -> invalid("offset must not be negative.")
        page.limit !in MIN_LIMIT..MAX_LIMIT -> invalid("limit must be between $MIN_LIMIT and $MAX_LIMIT.")
        page.maxChars !in MIN_CHARS..MAX_CHARS -> invalid("maxChars must be between $MIN_CHARS and $MAX_CHARS.")
        page.offset > 0 && page.expectedRevision == null ->
            invalid("expectedRevision is required for a continuation; start again at offset 0.")

        else -> null
    }

    /** An invalid budget cannot bound its own error, so such a response is written within a fixed small one. */
    private fun budgetFor(page: BeanPageRequest): Int =
        if (page.maxChars in MIN_CHARS..MAX_CHARS) page.maxChars else FALLBACK_BUDGET

    private fun invalid(message: String) = BeanQueryProblem(INVALID_ARGUMENT, message)

    private fun resultChanged() = BeanQueryProblem(
        RESULT_CHANGED,
        "The model or the query changed since the previous page; start again at offset 0."
    )

    private fun tooLarge(minimumRequiredChars: Int): BeanQueryProblem {
        val message = if (minimumRequiredChars > MAX_CHARS) {
            "This record cannot be returned within the maximum supported maxChars of $MAX_CHARS."
        } else {
            "Increase maxChars to at least $minimumRequiredChars."
        }
        return BeanQueryProblem(RESPONSE_TOO_LARGE, message)
    }

    private fun encodedLength(node: ObjectNode): Int = mapper.writeValueAsString(node).length

    private val mapper = ObjectMapper()

    companion object {
        const val INVALID_ARGUMENT = "INVALID_ARGUMENT"
        const val RESULT_CHANGED = "RESULT_CHANGED"
        const val RESPONSE_TOO_LARGE = "RESPONSE_TOO_LARGE"

        const val MIN_LIMIT = 1
        const val MAX_LIMIT = 50
        const val MIN_CHARS = 512
        const val MAX_CHARS = 16000

        /** Budget for an error that reports a broken budget; small enough to fit any client's floor. */
        const val FALLBACK_BUDGET = 512

        private const val STATUS_OK = "OK"
        private const val STATUS_ERROR = "ERROR"
        private const val FIELD_STATUS = "status"
        private const val FIELD_ERROR = "error"
        private const val FIELD_REVISION = "revision"
        private const val FIELD_TOTAL_COUNT = "totalCount"
        private const val FIELD_TRUNCATED = "truncated"
        private const val FIELD_NEXT_OFFSET = "nextOffset"
        private const val FIELD_CANDIDATES = "candidates"
    }
}
