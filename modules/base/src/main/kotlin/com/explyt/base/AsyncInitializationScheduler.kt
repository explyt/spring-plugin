/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.progress.ProcessCanceledException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Runs one deferred initialization at a time and closes races at the buffer hand-off boundary. */
internal class AsyncInitializationScheduler(
    private val executor: Executor,
    private val initialize: () -> Unit,
    private val flush: () -> Unit,
    private val isInitialized: () -> Boolean,
    private val hasPending: () -> Boolean,
    private val discardPending: () -> Unit,
) {
    private val scheduled = AtomicBoolean(false)

    fun schedule() {
        if (isInitialized()) {
            flush()
            return
        }
        if (!scheduled.compareAndSet(false, true)) return

        executor.execute {
            try {
                initialize()
                flush()
            } catch (exception: ProcessCanceledException) {
                throw exception
            } catch (_: Exception) {
                discardPending()
            } finally {
                scheduled.set(false)
                if (!isInitialized() && hasPending()) schedule()
            }
        }
    }
}
