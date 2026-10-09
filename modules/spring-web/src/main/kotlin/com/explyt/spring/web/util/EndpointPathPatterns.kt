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

    val SPECIFICITY: Comparator<String> = compareBy<String> { isCatchAll(it) }
        .thenBy { score(it) }
        .thenByDescending { it.length }
        .thenBy { it }

    fun segments(path: String): List<String> = path.split('/').filter { it.isNotEmpty() }

    fun isTemplateSegment(segment: String): Boolean = segment.startsWith('{') || segment.contains('*')

    fun isCaptureRest(segment: String): Boolean =
        segment == CATCH_ALL ||
                segment.length > CAPTURE_REST_OPENING.length + 1 &&
                segment.startsWith(CAPTURE_REST_OPENING) && segment.endsWith('}')

    fun isCatchAll(path: String): Boolean = segments(path).any(::isCaptureRest)

    fun <R, E> preferredReading(
        readings: Sequence<R>,
        matchesOf: (R) -> List<E>,
        pathOf: (E) -> String,
    ): Pair<R, List<E>>? {
        var catchAllOnly: Pair<R, List<E>>? = null
        for (reading in readings) {
            val matches = matchesOf(reading)
            when {
                matches.isEmpty() -> continue
                matches.any { !isCatchAll(pathOf(it)) } -> return reading to matches
                catchAllOnly == null -> catchAllOnly = reading to matches
            }
        }
        return catchAllOnly
    }

    /**
     * How many leading segments [path] and [other] have in common, a template segment on either side matching
     * any segment on the other: `/api/routes/{id}` and `/api/routes/export/preview` share three. A capture-rest
     * segment matches every remaining segment on the other side.
     */
    fun sharedLeadingSegments(path: String, other: String): Int {
        val ours = segments(path)
        val theirs = segments(other)
        ours.zip(theirs).forEachIndexed { index, (a, b) ->
            when {
                isCaptureRest(a) -> return theirs.size
                isCaptureRest(b) -> return ours.size
                a != b && !isTemplateSegment(a) && !isTemplateSegment(b) -> return index
            }
        }
        return minOf(ours.size, theirs.size)
    }

    fun prefixOf(path: String, segmentCount: Int): String =
        segments(path).take(segmentCount).joinToString(separator = "/", prefix = "/")

    /**
     * The request path of a URL as a client writes it - in a browser, a log line or a curl: no route declares a
     * scheme, a host, a query or a fragment, so `https://example.com/t/api/items?window=24h#top` is `/t/api/items`.
     */
    fun requestPathOf(url: String): String =
        SpringWebUtil.simplifyUrl(url.trim().replace(ORIGIN, "").substringBefore('#'))

    /**
     * The ways a request [path] can meet the routes: as given first, then under a leading prefix no route declares,
     * fewest dropped segments first - `/t/api/items` as given, as `/api/items` under `/t`, as `/items` under `/t/api`.
     *
     * A servlet context path, a gateway route or an ingress rule prepends such a prefix, and it usually lives in
     * deployment configuration only, where no endpoint model can see it.
     */
    fun readingsOf(path: String): Sequence<PathReading> {
        val segments = segments(path)
        return sequenceOf(PathReading(assumedPrefix = null, path = path)) +
                (1 until segments.size).asSequence().map { dropped ->
                    PathReading(
                        assumedPrefix = segments.take(dropped).joinToString(separator = "/", prefix = "/"),
                        path = segments.drop(dropped).joinToString(separator = "/", prefix = "/"),
                    )
                }
    }

    /**
     * A request path as it meets the routes, with the leading [assumedPrefix] dropped from it, or `null` when it is
     * read as given.
     */
    data class PathReading(val assumedPrefix: String?, val path: String) {

        /**
         * Whether [route] may be what follows the dropped prefix: it opens with the literal segment [path] opens with.
         *
         * A route opening with a `{template}` would claim whatever a dropped prefix left over, which makes a match
         * under an assumed prefix evidence of nothing. A path read as given admits every route.
         */
        fun admits(route: String): Boolean {
            if (assumedPrefix == null) return true
            val opening = segments(route).firstOrNull() ?: return false
            return !isTemplateSegment(opening) && opening == segments(path).firstOrNull()
        }
    }

    private fun score(path: String): Int =
        segments(path).sumOf { segment ->
            when {
                segment.contains('*') || segment.contains('?') -> WILDCARD_WEIGHT
                segment.startsWith('{') -> CAPTURE_WEIGHT
                else -> 0
            }
        }

    private val ORIGIN = Regex("^[A-Za-z][A-Za-z0-9+.-]*://[^/?#]*")
    private const val CATCH_ALL = "**"
    private const val CAPTURE_REST_OPENING = "{*"
    private const val WILDCARD_WEIGHT = 100
    private const val CAPTURE_WEIGHT = 1
}
