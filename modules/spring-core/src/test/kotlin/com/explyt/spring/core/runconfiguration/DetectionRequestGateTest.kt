/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class DetectionRequestGateTest {
    @Test
    fun testCoalescesRequestsWhileDetectionIsRunning() {
        val startedDetections = AtomicInteger()
        val gate = DetectionRequestGate { startedDetections.incrementAndGet() }

        gate.request()
        gate.request()
        gate.request()

        assertEquals(1, startedDetections.get())

        gate.completed()

        assertEquals(2, startedDetections.get())

        gate.completed()

        assertEquals(2, startedDetections.get())
    }
}
