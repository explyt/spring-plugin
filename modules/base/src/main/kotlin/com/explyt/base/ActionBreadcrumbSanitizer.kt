/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import io.sentry.Breadcrumb
import java.util.Date

/** Makes action breadcrumb trails useful at event-capture time without mutating Sentry's global scope queue. */
object ActionBreadcrumbSanitizer {

    const val ACTION_CATEGORY = "action"
    private const val PLACE_KEY = "place"
    private const val COUNT_KEY = "count"
    private const val MAX_AGE_MILLIS = 15 * 60 * 1000L

    /**
     * Drops stale/noisy action entries and collapses adjacent equal actions. Non-action breadcrumbs are preserved.
     *
     * The transformation is pure from the caller's point of view: repeated actions are represented by copied
     * breadcrumbs, so the objects held by Sentry's global scope are never modified while an event is being serialized.
     */
    fun sanitize(breadcrumbs: List<Breadcrumb>, now: Date): List<Breadcrumb> {
        val cutoff = now.time - MAX_AGE_MILLIS
        val result = ArrayList<Breadcrumb>(breadcrumbs.size)

        for (breadcrumb in breadcrumbs) {
            if (breadcrumb.category != ACTION_CATEGORY) {
                result += breadcrumb
                continue
            }

            if (breadcrumb.timestamp.time < cutoff || !ActionBreadcrumbPolicy.shouldRecord(breadcrumb.message)) {
                continue
            }

            val previous = result.lastOrNull()
            if (previous != null && sameAction(previous, breadcrumb)) {
                val count = (previous.getData(COUNT_KEY) as? Number)?.toInt() ?: 1
                result[result.lastIndex] = copyWithCount(previous, count + 1)
            } else {
                result += breadcrumb
            }
        }

        return result
    }

    private fun sameAction(first: Breadcrumb, second: Breadcrumb): Boolean =
        first.category == ACTION_CATEGORY &&
                first.message == second.message &&
                first.getData(PLACE_KEY) == second.getData(PLACE_KEY)

    private fun copyWithCount(source: Breadcrumb, count: Int): Breadcrumb =
        Breadcrumb(source.timestamp).apply {
            message = source.message
            type = source.type
            category = source.category
            origin = source.origin
            level = source.level
            source.getData(PLACE_KEY)?.let { setData(PLACE_KEY, it) }
            setData(COUNT_KEY, count)
        }
}
