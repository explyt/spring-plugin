/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

/**
 * Suppresses a run of identical consecutive actions before it reaches Sentry.
 *
 * Sentry keeps only the last 100 breadcrumbs, so collapsing a repeat at send time is too late: the repeat has already
 * evicted the navigation entries that explain a failure. Suppressing it here keeps one queue slot per run instead
 * of one per keypress.
 */
internal class RepeatedActionSuppressor {

    private var lastKey: String? = null

    /**
     * Returns whether an action identified by [actionName] at [place] should be recorded.
     *
     * Called from the EDT for every performed action, so it does no more than compare and store one string.
     */
    @Synchronized
    fun shouldRecord(actionName: String, place: String?): Boolean {
        val key = "$actionName@$place"
        if (key == lastKey) return false
        lastKey = key
        return true
    }
}
