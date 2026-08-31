/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs one deferred initialization at a time, closing the races at the producer hand-off boundary.
 *
 * Initialization is expensive, so a failure is retried only [MAX_ATTEMPTS] times: resetting the state unconditionally
 * would make every subsequent producer pay that cost again, turning one failure into a retry storm.
 */
internal class AsyncInitializationScheduler(
    private val executor: Executor,
    private val initialize: () -> Unit,
    private val flush: () -> Unit,
    private val isInitialized: () -> Boolean,
    private val hasPending: () -> Boolean,
    private val discardPending: () -> Unit,
) {
    private val running = AtomicBoolean(false)
    private val attempts = AtomicInteger(0)

    fun schedule() {
        if (isInitialized()) {
            flush()
            return
        }
        if (attempts.get() >= MAX_ATTEMPTS) return
        if (!running.compareAndSet(false, true)) return

        try {
            executor.execute {
                var shouldScheduleFollowUp = true
                try {
                    attempts.incrementAndGet()
                    initialize()
                    flush()
                } catch (exception: ProcessCanceledException) {
                    // Cancellation is not an initialization failure: rethrow it, but do not spend the retry budget on
                    // a follow-up pass the caller never asked for.
                    shouldScheduleFollowUp = false
                    throw exception
                } catch (exception: Exception) {
                    shouldScheduleFollowUp = false
                    // Best-effort telemetry: buffered data is dropped rather than retained until the process exits.
                    discardPending()
                    logger.warn("Failed to initialize error reporting, attempt ${attempts.get()} of $MAX_ATTEMPTS", exception)
                } finally {
                    running.set(false)
                    if (shouldScheduleFollowUp && !isInitialized() && hasPending()) schedule()
                }
            }
        } catch (_: RejectedExecutionException) {
            // No task was accepted, so clear the in-flight flag and let a later producer retry instead of surfacing
            // the rejection on the calling thread, which is the EDT.
            running.set(false)
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 3

        private val logger = Logger.getInstance(AsyncInitializationScheduler::class.java)
    }
}
