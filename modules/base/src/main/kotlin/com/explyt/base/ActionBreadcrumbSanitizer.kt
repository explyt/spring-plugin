/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.diagnostic.Logger
import io.sentry.Breadcrumb
import java.util.Date
import kotlin.time.Duration.Companion.minutes

/** Makes action breadcrumb trails readable at event-capture time without mutating Sentry's global scope queue. */
object ActionBreadcrumbSanitizer {

    const val ACTION_CATEGORY = "action"

    private const val PLACE_KEY = "place"
    private const val COUNT_KEY = "count"
    private const val FIRST_SEEN_KEY = "first_seen"

    private val MAX_AGE_MILLIS = 15.minutes.inWholeMilliseconds

    /** Kept when the age filter would otherwise leave a report with no reproduction path at all. */
    private const val FALLBACK_KEPT_ACTIONS = 10

    private val logger = Logger.getInstance(ActionBreadcrumbSanitizer::class.java)

    /**
     * Sanitizes [breadcrumbs], falling back to the untouched trail on any failure.
     *
     * Sentry drops the whole event when a `beforeSend` callback throws, so a defect here would silently cost the
     * report itself. Losing the improvement is acceptable; losing the error is not.
     */
    fun sanitizeSafely(breadcrumbs: List<Breadcrumb>, eventTime: Date): List<Breadcrumb> =
        runCatching { sanitize(breadcrumbs, eventTime) }
            .onFailure { logger.warn("Failed to sanitize Sentry action breadcrumbs", it) }
            .getOrDefault(breadcrumbs)

    /**
     * Drops stale and noisy action entries and collapses adjacent equal actions. Other categories are preserved.
     *
     * Age is measured from [eventTime] rather than from the current time: a report is often submitted long after the
     * failure, and measuring from "now" would erase the entire trail of the event being reported.
     *
     * Repeated actions are represented by *copies*, so the breadcrumbs held by Sentry's global scope are never
     * modified while an event is being serialized on another thread.
     */
    fun sanitize(breadcrumbs: List<Breadcrumb>, eventTime: Date): List<Breadcrumb> {
        val cutoff = eventTime.time - MAX_AGE_MILLIS
        val byAge = collapseActions(breadcrumbs) { _, breadcrumb -> breadcrumb.timestampMillis() >= cutoff }
        if (byAge.any { it.category == ACTION_CATEGORY }) return byAge

        // Every action is older than the boundary — typically a report submitted long after the failure. Returning a
        // trail with no actions at all would be worse than returning an old one, so keep the newest few.
        val oldestKeptIndex = breadcrumbs.indexOfLastActions(FALLBACK_KEPT_ACTIONS)
        return collapseActions(breadcrumbs) { index, _ -> index >= oldestKeptIndex }
    }

    private inline fun collapseActions(
        breadcrumbs: List<Breadcrumb>,
        keep: (Int, Breadcrumb) -> Boolean,
    ): List<Breadcrumb> {
        val result = ArrayList<Breadcrumb>(breadcrumbs.size)

        breadcrumbs.forEachIndexed { index, breadcrumb ->
            if (breadcrumb.category != ACTION_CATEGORY) {
                result += breadcrumb
                return@forEachIndexed
            }
            if (!ActionBreadcrumbPolicy.shouldRecord(breadcrumb.message)) return@forEachIndexed
            if (!keep(index, breadcrumb)) return@forEachIndexed

            val previous = result.lastOrNull()
            if (previous != null && sameAction(previous, breadcrumb)) {
                result[result.lastIndex] = collapse(previous, breadcrumb)
            } else {
                result += breadcrumb
            }
        }

        return result
    }

    /** Index of the oldest action breadcrumb within the newest [count] of them. */
    private fun List<Breadcrumb>.indexOfLastActions(count: Int): Int {
        var remaining = count
        for (index in indices.reversed()) {
            if (this[index].category != ACTION_CATEGORY) continue
            if (--remaining <= 0) return index
        }
        return 0
    }

    private fun sameAction(first: Breadcrumb, second: Breadcrumb): Boolean =
        first.category == ACTION_CATEGORY &&
                first.message == second.message &&
                first.getData(PLACE_KEY) == second.getData(PLACE_KEY)

    /**
     * Times the group by its newest occurrence, so "the last action before the failure" is not reported too early,
     * and keeps the start of the run under [FIRST_SEEN_KEY].
     */
    private fun collapse(group: Breadcrumb, next: Breadcrumb): Breadcrumb {
        val count = (group.getData(COUNT_KEY) as? Number)?.toInt() ?: 1
        val firstSeen = group.getData(FIRST_SEEN_KEY) ?: group.timestamp.toString()
        return copyOf(group, next.timestamp).apply {
            setData(COUNT_KEY, count + 1)
            setData(FIRST_SEEN_KEY, firstSeen)
        }
    }

    @Suppress("UnstableApiUsage")
    private fun copyOf(source: Breadcrumb, timestamp: Date): Breadcrumb =
        Breadcrumb(timestamp).apply {
            message = source.message
            type = source.type
            category = source.category
            origin = source.origin
            level = source.level
            source.data.forEach { (key, value) -> setData(key, value) }
        }

    /** [Breadcrumb.getTimestamp] throws when a breadcrumb carries no time; such an entry must not be aged out. */
    private fun Breadcrumb.timestampMillis(): Long =
        runCatching { timestamp.time }.getOrDefault(Long.MAX_VALUE)
}
