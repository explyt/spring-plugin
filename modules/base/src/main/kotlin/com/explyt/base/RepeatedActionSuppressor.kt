/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import java.util.Date

/**
 * Suppresses a run of identical consecutive actions before it reaches Sentry.
 *
 * Sentry keeps only the last 100 breadcrumbs, so collapsing a repeat at send time is too late: the repeat has already
 * evicted the navigation entries that explain a failure. Suppressing it here keeps one queue slot per run instead
 * of one per keypress, and the suppressed repeats are reported once, when their run ends, so the collapsed trail still
 * counts them and still shows when the last one happened.
 */
internal class RepeatedActionSuppressor {

    /** The repeats of one recorded action that were not recorded themselves. */
    data class SuppressedRepeats(val actionName: String, val place: String?, val count: Int, val lastSeen: Date)

    /** Whether to record the action itself, and the repeats of the run it ended, if any. */
    data class Decision(val record: Boolean, val endedRun: SuppressedRepeats?)

    private var current: Run? = null

    /** Called from the EDT for every recorded action, so it only compares and stores a few fields. */
    @Synchronized
    fun onAction(actionName: String, place: String?, at: Date): Decision {
        val run = current
        if (run != null && run.actionName == actionName && run.place == place) {
            run.repeat(at)
            return Decision(record = false, endedRun = null)
        }
        current = Run(actionName, place)
        return Decision(record = true, endedRun = run?.suppressed())
    }

    /**
     * Ends the current run on an action that is not recorded at all - typing between two `GotoDeclaration` calls
     * makes them two user steps, and the second must not vanish as a repeat of the first.
     */
    @Synchronized
    fun breakRun(): SuppressedRepeats? = current?.suppressed().also { current = null }

    private class Run(val actionName: String, val place: String?) {
        private var repeats = 0
        private var lastSeen: Date? = null

        fun repeat(at: Date) {
            repeats++
            lastSeen = at
        }

        fun suppressed(): SuppressedRepeats? = lastSeen?.let { SuppressedRepeats(actionName, place, repeats, it) }
    }
}
