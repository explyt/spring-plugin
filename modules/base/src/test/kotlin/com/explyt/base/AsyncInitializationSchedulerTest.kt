/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.ArrayDeque
import java.util.concurrent.Executor

class AsyncInitializationSchedulerTest {

    @Test
    fun `flushes work offered while the buffer is being drained`() {
        val tasks = ArrayDeque<Runnable>()
        var initialized = false
        var pending = true
        var flushes = 0
        lateinit var scheduler: AsyncInitializationScheduler
        scheduler = AsyncInitializationScheduler(
            executor = { tasks.addLast(it) },
            initialize = { initialized = true },
            flush = {
                flushes++
                pending = false
                if (flushes == 1) {
                    // A producer offers an item after the drain but before the worker returns.
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

    /** A failure must not strand the buffer, but the retry has to be arranged by the scheduler itself. */
    @Test
    fun `retries after a failed initialization`() {
        val tasks = ArrayDeque<Runnable>()
        var attempts = 0
        var pending = true
        val scheduler = AsyncInitializationScheduler(
            executor = { tasks.addLast(it) },
            initialize = {
                attempts++
                error("initialization failed")
            },
            flush = { pending = false },
            isInitialized = { false },
            hasPending = { pending },
            discardPending = { pending = false },
        )

        scheduler.schedule()
        tasks.removeFirst().run()

        assertEquals(1, attempts)
        assertFalse("pending telemetry is best-effort and must be dropped", pending)
    }

    /**
     * Initialization reads the manifest of every installed plugin, so an unbounded retry would repeat that cost for
     * every subsequent action. After the attempt budget is spent the scheduler must stay silent.
     */
    @Test
    fun `stops attempting after the retry budget is spent`() {
        val tasks = ArrayDeque<Runnable>()
        var attempts = 0
        val scheduler = AsyncInitializationScheduler(
            executor = { tasks.addLast(it) },
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
