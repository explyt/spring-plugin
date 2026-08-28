/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import com.intellij.openapi.progress.ProcessCanceledException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.ArrayDeque
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

class AsyncInitializationSchedulerTest {

    @Test
    fun `flushes work offered while the buffer is being drained`() {
        val tasks = ArrayDeque<Runnable>()
        var initialized = false
        var pending = true
        var flushes = 0
        lateinit var scheduler: AsyncInitializationScheduler
        scheduler = AsyncInitializationScheduler(
            executor = Executor { tasks.addLast(it) },
            initialize = { initialized = true },
            flush = {
                flushes++
                pending = false
                if (flushes == 1) {
                    pending = true
                    scheduler.schedule()
                }
            },
            isInitialized = { initialized },
            hasPending = { pending },
            discardPending = { pending = false },
        )

        scheduler.schedule()
        tasks.removeFirst().run()

        assertEquals("the late item must be flushed, not stranded", 2, flushes)
        assertFalse("nothing may remain pending", pending)
    }

    @Test
    fun `a failed initialization allows a later attempt`() {
        val tasks = ArrayDeque<Runnable>()
        var initialized = false
        var shouldFail = true
        val scheduler = AsyncInitializationScheduler(
            executor = Executor { tasks.addLast(it) },
            initialize = {
                if (shouldFail) {
                    shouldFail = false
                    error("initialization failed")
                }
                initialized = true
            },
            flush = {},
            isInitialized = { initialized },
            hasPending = { false },
            discardPending = {},
        )

        scheduler.schedule()
        tasks.removeFirst().run()
        scheduler.schedule()
        tasks.removeFirst().run()

        assertEquals(true, initialized)
    }

    @Test
    fun `executor rejection allows a later initialization attempt`() {
        val tasks = ArrayDeque<Runnable>()
        var reject = true
        val scheduler = AsyncInitializationScheduler(
            executor = Executor { command ->
                if (reject) {
                    reject = false
                    throw RejectedExecutionException("executor is shutting down")
                }
                tasks.addLast(command)
            },
            initialize = {},
            flush = {},
            isInitialized = { false },
            hasPending = { false },
            discardPending = {},
        )

        var rejectionEscaped = false
        try {
            scheduler.schedule()
        } catch (_: RejectedExecutionException) {
            rejectionEscaped = true
        }

        assertFalse("executor rejection must not reach the caller", rejectionEscaped)
        scheduler.schedule()
        assertEquals("a later attempt must be accepted", 1, tasks.size)
    }

    @Test
    fun `stops attempting after the retry budget is spent`() {
        val tasks = ArrayDeque<Runnable>()
        var attempts = 0
        val scheduler = AsyncInitializationScheduler(
            executor = Executor { tasks.addLast(it) },
            initialize = {
                attempts++
                error("initialization failed")
            },
            flush = {},
            isInitialized = { false },
            hasPending = { true },
            discardPending = {},
        )

        repeat(10) {
            scheduler.schedule()
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }

        assertEquals(AsyncInitializationScheduler.MAX_ATTEMPTS, attempts)
    }

    @Test
    fun `cancellation is rethrown without scheduling a follow-up`() {
        val tasks = ArrayDeque<Runnable>()
        val scheduler = AsyncInitializationScheduler(
            executor = { tasks.addLast(it) },
            initialize = { throw ProcessCanceledException() },
            flush = {},
            isInitialized = { false },
            hasPending = { true },
            discardPending = {},
        )

        scheduler.schedule()
        var cancellationEscaped = false
        try {
            tasks.removeFirst().run()
        } catch (_: ProcessCanceledException) {
            cancellationEscaped = true
        }

        assertEquals(true, cancellationEscaped)
        assertEquals("cancellation must not enqueue a follow-up", 0, tasks.size)
    }

    @Test
    fun `runs no initialization when it already happened`() {
        val tasks = ArrayDeque<Runnable>()
        var flushes = 0
        val scheduler = AsyncInitializationScheduler(
            executor = { tasks.addLast(it) },
            initialize = { error("must not initialize twice") },
            flush = { flushes++ },
            isInitialized = { true },
            hasPending = { false },
            discardPending = {},
        )

        scheduler.schedule()

        assertEquals(1, flushes)
        assertEquals("no background work is needed", 0, tasks.size)
    }
}
