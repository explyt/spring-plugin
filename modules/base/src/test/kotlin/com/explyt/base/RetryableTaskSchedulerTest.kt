/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Contract for the non-blocking scheduler used to hand Sentry initialization off the EDT.
 */
class RetryableTaskSchedulerTest {

    @Test
    fun `only one scheduled task may run until it completes`() {
        val executor = QueuedExecutor()
        val scheduler = RetryableTaskScheduler(executor)
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)

        assertTrue(scheduler.schedule {
            started.countDown()
            assertTrue("test task must be released", release.await(30, TimeUnit.SECONDS))
        })

        val runningTask = thread(start = true) { executor.runNext() }
        assertTrue("scheduled task must start", started.await(30, TimeUnit.SECONDS))

        assertFalse("a second task must not be accepted while the first is in flight", scheduler.schedule {})

        release.countDown()
        runningTask.join(TimeUnit.SECONDS.toMillis(30))
        assertFalse("scheduled task must finish", runningTask.isAlive)

        assertTrue("a task must be accepted after the previous task completes", scheduler.schedule {})
        assertEquals(1, executor.queuedTaskCount)
    }

    @Test
    fun `a task failure allows a later task to be scheduled`() {
        val executor = QueuedExecutor()
        val scheduler = RetryableTaskScheduler(executor)
        val failure = IllegalStateException("initialization failed")

        assertTrue(scheduler.schedule { throw failure })

        assertSame(failure, assertThrows(IllegalStateException::class.java) { executor.runNext() })

        assertTrue("a failed task must not permanently block later scheduling", scheduler.schedule {})
        assertEquals(1, executor.queuedTaskCount)
    }

    @Test
    fun `executor rejection does not escape and a later task is accepted`() {
        val executor = RejectOnceExecutor()
        val scheduler = RetryableTaskScheduler(executor)

        assertFalse("a rejected task must report that it was not scheduled", scheduler.schedule {})

        assertTrue("executor rejection must not permanently block later scheduling", scheduler.schedule {})
        assertEquals(1, executor.queuedTaskCount)
    }

    private class QueuedExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()

        override fun execute(command: Runnable) {
            tasks.addLast(command)
        }

        val queuedTaskCount: Int
            get() = tasks.size

        fun runNext() {
            tasks.removeFirst().run()
        }
    }

    private class RejectOnceExecutor : Executor {
        private var rejected = false
        private val acceptedTasks = ArrayDeque<Runnable>()

        override fun execute(command: Runnable) {
            if (!rejected) {
                rejected = true
                throw RejectedExecutionException("executor is shutting down")
            }
            acceptedTasks.addLast(command)
        }

        val queuedTaskCount: Int
            get() = acceptedTasks.size
    }
}
