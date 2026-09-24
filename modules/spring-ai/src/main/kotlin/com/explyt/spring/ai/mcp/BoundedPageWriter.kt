/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/** Which slice of an answer to serve, and within which budget. */
data class PageRequest(
    val offset: Int = 0,
    val limit: Int = 5,
    val maxChars: Int = 1800,
    val expectedRevision: String? = null
)

/**
 * Serializes one page of an answer within a character budget.
 *
 * The budget is measured on the *finished* compact JSON, metadata and continuation included, because that is
 * what the client receives: slicing N items and assuming the envelope fits would overshoot exactly when the
 * paths are long. Items are appended one at a time and the whole envelope is re-serialized after each, so the
 * first one that does not fit ends the page instead of truncating a value.
 *
 * The envelope is supplied by the caller and describes its own answer; this writer only adds what paging means
 * - `status`, `revision`, `totalCount`, `offset`, `truncated`, `nextOffset` - and the array of items.
 */
class BoundedPageWriter {

    /**
     * @param envelope the caller's own fields; copied rather than kept, so one template can serve several pages.
     * @param itemsField name of the array the items are served under.
     * @param itemAt projects a single item on demand instead of taking a finished list: a page of five must not
     *   pay for the details of every record. It is called at most once per item the page attempts to serve.
     */
    fun write(
        envelope: ObjectNode,
        itemsField: String,
        totalCount: Int,
        itemAt: (Int) -> ObjectNode,
        revision: String,
        page: PageRequest
    ): String {
        validate(page)?.let { return writeError(it.first, it.second, emptyList(), budgetFor(page)) }
        page.expectedRevision?.let {
            if (it != revision) return writeError(
                RESULT_CHANGED,
                "The model or the query changed since the previous page; start again at offset 0.",
                emptyList(),
                budgetFor(page)
            )
        }

        val root = pageEnvelope(envelope, revision, totalCount, page.offset)
        val items = root.putArray(itemsField)
        if (encodedLength(root) > page.maxChars) {
            return writeTooLarge(encodedLength(root))
        }

        var added = 0
        while (added < page.limit && page.offset + added < totalCount) {
            items.add(itemAt(page.offset + added))
            setContinuation(root, totalCount, page.offset + added + 1)
            if (encodedLength(root) > page.maxChars) {
                items.remove(items.size() - 1)
                break
            }
            added++
        }
        setContinuation(root, totalCount, page.offset + added)

        if (added == 0 && page.offset < totalCount) {
            return writeTooLarge(minimumFor(envelope, itemsField, totalCount, itemAt, revision, page))
        }
        return mapper.writeValueAsString(root)
    }

    fun writeError(
        code: String,
        message: String,
        choices: List<Map<String, String>> = emptyList(),
        maxChars: Int
    ): String {
        val root = mapper.createObjectNode()
        root.put(FIELD_STATUS, STATUS_ERROR)
        val error = root.putObject(FIELD_ERROR)
        error.put("code", code)
        error.put("message", message)
        val choiceArray = error.putArray("choices")
        error.put(FIELD_TRUNCATED, false)

        for ((index, choice) in choices.withIndex()) {
            choiceArray.add(mapper.valueToTree<ObjectNode>(choice))
            error.put(FIELD_TRUNCATED, index + 1 < choices.size)
            if (encodedLength(root) > maxChars) {
                choiceArray.remove(choiceArray.size() - 1)
                error.put(FIELD_TRUNCATED, true)
                break
            }
        }
        return mapper.writeValueAsString(root)
    }

    /**
     * The envelope without items: everything whose size does not depend on the page.
     *
     * `truncated` and `nextOffset` are written here with their widest shape - a number, not `null` - so that
     * adding the continuation later can only shrink the response, never push an already-measured page over the
     * budget.
     */
    private fun pageEnvelope(envelope: ObjectNode, revision: String, totalCount: Int, offset: Int): ObjectNode {
        val root = mapper.createObjectNode()
        root.put(FIELD_STATUS, STATUS_OK)
        root.setAll<ObjectNode>(envelope.deepCopy())
        root.put(FIELD_REVISION, revision)
        root.put(FIELD_TOTAL_COUNT, totalCount)
        root.put(FIELD_OFFSET, offset)
        root.put(FIELD_TRUNCATED, true)
        root.put(FIELD_NEXT_OFFSET, totalCount)
        return root
    }

    /**
     * Points at the record after the ones served, or terminates the continuation.
     *
     * Applied after every appended item so the measured envelope carries the continuation it will be sent with,
     * and once more after the loop: the page that served nothing - an offset past the end - still has to replace
     * the placeholder the envelope was measured with.
     */
    private fun setContinuation(root: ObjectNode, totalCount: Int, served: Int) {
        val remaining = served < totalCount
        root.put(FIELD_TRUNCATED, remaining)
        if (remaining) root.put(FIELD_NEXT_OFFSET, served) else root.putNull(FIELD_NEXT_OFFSET)
    }

    /**
     * Size of an envelope holding just the next item, so a caller learns the budget that would fetch it.
     *
     * Reported instead of the item itself: repeating an element that already overflowed the response is the one
     * thing the budget exists to prevent.
     */
    private fun minimumFor(
        envelope: ObjectNode,
        itemsField: String,
        totalCount: Int,
        itemAt: (Int) -> ObjectNode,
        revision: String,
        page: PageRequest
    ): Int {
        val probe = pageEnvelope(envelope, revision, totalCount, page.offset)
        probe.putArray(itemsField).add(itemAt(page.offset))
        return encodedLength(probe)
    }

    /** Code and message of the first broken argument, or `null` when the request can be served. */
    private fun validate(page: PageRequest): Pair<String, String>? = when {
        page.offset < 0 -> invalid("offset must not be negative.")
        page.limit !in MIN_LIMIT..MAX_LIMIT -> invalid("limit must be between $MIN_LIMIT and $MAX_LIMIT.")
        page.maxChars !in MIN_CHARS..MAX_CHARS -> invalid("maxChars must be between $MIN_CHARS and $MAX_CHARS.")
        page.offset > 0 && page.expectedRevision == null ->
            invalid("expectedRevision is required for a continuation; start again at offset 0.")

        else -> null
    }

    /** An invalid budget cannot bound its own error, so such a response is written within a fixed small one. */
    private fun budgetFor(page: PageRequest): Int =
        if (page.maxChars in MIN_CHARS..MAX_CHARS) page.maxChars else FALLBACK_BUDGET

    private fun invalid(message: String) = INVALID_ARGUMENT to message

    private fun writeTooLarge(minimumRequiredChars: Int): String {
        val message = if (minimumRequiredChars > MAX_CHARS) {
            "This record cannot be returned within the maximum supported maxChars of $MAX_CHARS."
        } else {
            "Increase maxChars to at least $minimumRequiredChars."
        }
        return writeError(RESPONSE_TOO_LARGE, message, emptyList(), FALLBACK_BUDGET)
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
        private const val FIELD_OFFSET = "offset"
        private const val FIELD_TRUNCATED = "truncated"
        private const val FIELD_NEXT_OFFSET = "nextOffset"
    }
}
