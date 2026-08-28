/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Schedules at most one task at a time and allows a later attempt after task or executor failure.
 */
internal class RetryableTaskScheduler(private val executor: Executor) {

    private val scheduled = AtomicBoolean(false)

    /**
     * Schedules [task] unless another task is already queued or running.
     *
     * The task's exception is deliberately not swallowed. The scheduler only owns the in-flight state; callers decide
     * how task failures should be observed. Executor rejection is different: it means no task was accepted, so the
     * state is reset and the rejection is reported as `false` without escaping the caller.
     */
    fun schedule(task: () -> Unit): Boolean {
        if (!scheduled.compareAndSet(false, true)) return false

        try {
            executor.execute {
                try {
                    task()
                } finally {
                    scheduled.set(false)
                }
            }
            return true
        } catch (_: RejectedExecutionException) {
            scheduled.set(false)
            return false
        }
    }
}
