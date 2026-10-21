/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests for [BoundedBuffer], which holds action breadcrumbs recorded before Sentry has finished initializing.
 */
class BoundedBufferTest {

    @Test
    fun `drain returns items in insertion order`() {
        val buffer = BoundedBuffer<String>(10)

        buffer.offer("first")
        buffer.offer("second")
        buffer.offer("third")

        assertEquals(listOf("first", "second", "third"), buffer.drain())
    }

    /** The buffer bounds memory while the IDE keeps producing actions, so the oldest breadcrumb is the one dropped. */
    @Test
    fun `oldest items are evicted when capacity is exceeded`() {
        val buffer = BoundedBuffer<Int>(3)

        repeat(5) { buffer.offer(it) }

        assertEquals(listOf(2, 3, 4), buffer.drain())
    }

    @Test
    fun `drain empties the buffer`() {
        val buffer = BoundedBuffer<String>(4)
        buffer.offer("only")

        assertEquals(listOf("only"), buffer.drain())

        assertTrue(buffer.isEmpty())
        assertEquals(emptyList<String>(), buffer.drain())
    }

    @Test
    fun `isEmpty reflects buffered content`() {
        val buffer = BoundedBuffer<String>(2)
        assertTrue(buffer.isEmpty())

        buffer.offer("value")

        assertFalse(buffer.isEmpty())
    }

    /**
     * Breadcrumbs are produced on the EDT while the pooled initialization thread drains them, so concurrent
     * `offer`/`drain` must neither lose an item nor deliver one twice.
     */
    @Test
    fun `concurrent offers and drains lose no items`() {
        val producerCount = 4
        val perProducer = 500
        val buffer = BoundedBuffer<Int>(producerCount * perProducer)
        val drained = mutableListOf<Int>()

        val executor = Executors.newFixedThreadPool(producerCount + 1)
        val startLine = CountDownLatch(1)
        val producersDone = CountDownLatch(producerCount)
        try {
            repeat(producerCount) { producer ->
                executor.execute {
                    startLine.await()
                    repeat(perProducer) { buffer.offer(producer * perProducer + it) }
                    producersDone.countDown()
                }
            }
            executor.execute {
                startLine.await()
                while (producersDone.count > 0L) {
                    synchronized(drained) { drained += buffer.drain() }
                }
            }

            startLine.countDown()
            assertTrue("producers must finish", producersDone.await(30, TimeUnit.SECONDS))
        } finally {
            executor.shutdown()
            assertTrue("executor must terminate", executor.awaitTermination(30, TimeUnit.SECONDS))
        }

        val observed = synchronized(drained) { drained.toList() } + buffer.drain()
        assertEquals("every item must be delivered exactly once", producerCount * perProducer, observed.size)
        assertEquals((0 until producerCount * perProducer).toSet(), observed.toSet())
    }
}
