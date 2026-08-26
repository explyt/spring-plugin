/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque
import java.util.concurrent.Executor

class AsyncInitializationSchedulerTest {

    @Test
    fun `reschedules work offered after the initialization flush`() {
        val tasks = ArrayDeque<Runnable>()
        var initialized = false
        var pending = true
        var flushes = 0
        val executor = Executor { tasks.addLast(it) }
        lateinit var scheduler: AsyncInitializationScheduler
        scheduler = AsyncInitializationScheduler(
            executor = executor,
            initialize = { initialized = true },
            flush = {
                flushes++
                pending = false
                if (flushes == 1) {
                    // Simulate a producer offering an item after the first drain but before it returns.
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

        assertEquals("work offered during the first flush must be flushed immediately", 2, flushes)
        assertTrue("all pending work must be flushed", !pending)
    }

    @Test
    fun `allows a new attempt after initialization fails`() {
        val tasks = ArrayDeque<Runnable>()
        var attempts = 0
        var pending = true
        val executor = Executor { tasks.addLast(it) }
        val scheduler = AsyncInitializationScheduler(
            executor = executor,
            initialize = {
                attempts++
                if (attempts == 1) error("initialization failed")
            },
            flush = { pending = false },
            isInitialized = { attempts > 1 },
            hasPending = { pending },
            discardPending = { pending = false },
        )

        scheduler.schedule()
        tasks.removeFirst().run()
        pending = true
        scheduler.schedule()
        tasks.removeFirst().run()

        assertEquals(2, attempts)
    }
}
