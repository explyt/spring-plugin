/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

/**
 * Ordering and proximity of endpoint path patterns, in the terms Spring itself uses to pick a handler.
 *
 * The specificity order follows `PathPattern.SPECIFICITY_COMPARATOR`: a catch-all sorts last, then the pattern
 * with fewer wildcards and captures wins, then the longer one. A list sorted this way answers "which mapping
 * wins for this URL" by its first element, which is what a caller asking about a URL that both a literal and a
 * `{template}` route match actually needs to know.
 */
object EndpointPathPatterns {

    val SPECIFICITY: Comparator<String> = compareBy<String> { it.contains(CATCH_ALL) }
        .thenBy { score(it) }
        .thenByDescending { it.length }
        .thenBy { it }

    fun segments(path: String): List<String> = path.split('/').filter { it.isNotEmpty() }

    fun isTemplateSegment(segment: String): Boolean = segment.startsWith('{') || segment.contains('*')

    /**
     * How many leading segments [path] and [other] have in common, a template segment on either side matching
     * any segment on the other: `/api/routes/{id}` and `/api/routes/export/preview` share three.
     */
    fun sharedLeadingSegments(path: String, other: String): Int =
        segments(path).zip(segments(other))
            .takeWhile { (a, b) -> a == b || isTemplateSegment(a) || isTemplateSegment(b) }
            .size

    fun prefixOf(path: String, segmentCount: Int): String =
        segments(path).take(segmentCount).joinToString(separator = "/", prefix = "/")

    private fun score(path: String): Int =
        segments(path).sumOf { segment ->
            when {
                segment.contains('*') || segment.contains('?') -> WILDCARD_WEIGHT
                segment.startsWith('{') -> CAPTURE_WEIGHT
                else -> 0
            }
        }

    private const val CATCH_ALL = "**"
    private const val WILDCARD_WEIGHT = 100
    private const val CAPTURE_WEIGHT = 1
}
