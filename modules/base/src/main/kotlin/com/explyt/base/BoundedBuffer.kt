/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.base

/**
 * A thread-safe FIFO buffer that keeps at most [capacity] most recent items, dropping the oldest ones.
 *
 * It exists so that a producer can hand work over without ever blocking on the consumer's slow setup: the lock here
 * only ever guards a list operation, never the initialization the consumer performs after draining.
 */
class BoundedBuffer<T>(private val capacity: Int) {

    init {
        require(capacity > 0) { "capacity must be positive but was $capacity" }
    }

    private val items = ArrayDeque<T>()

    /** Appends [item], evicting the oldest one when the buffer is full. */
    @Synchronized
    fun offer(item: T) {
        items.addLast(item)
        while (items.size > capacity) {
            items.removeFirst()
        }
    }

    /** Removes and returns everything buffered so far, in insertion order. */
    @Synchronized
    fun drain(): List<T> {
        if (items.isEmpty()) return emptyList()
        val drained = items.toList()
        items.clear()
        return drained
    }

    @Synchronized
    fun isEmpty(): Boolean = items.isEmpty()
}
