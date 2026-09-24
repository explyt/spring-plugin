/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanSnapshotIdentity

/**
 * Opaque version of one query's result: the model state it was read from, plus the question that was asked.
 *
 * A continuation is only the next page of the same answer when both still hold, so the caller sends it back and
 * a changed filter or a re-read model is reported as `RESULT_CHANGED` instead of silently serving a page of a
 * different result set. Paging parameters are deliberately excluded - `limit` and `maxChars` may change between
 * pages, and binding them would invalidate a continuation that describes the very same result.
 */
object BeanQueryRevision {

    fun compute(modelStamp: String, normalizedQuery: Map<String, String?>): String =
        BeanSnapshotIdentity.hash(
            listOf(modelStamp) + normalizedQuery.entries
                .sortedBy { it.key }
                .flatMap { listOf(it.key, it.value ?: NULL_MARKER) }
        )

    /** Distinguishes an absent key from one explicitly set to the string "null". */
    private const val NULL_MARKER = "\u0000null"
}
