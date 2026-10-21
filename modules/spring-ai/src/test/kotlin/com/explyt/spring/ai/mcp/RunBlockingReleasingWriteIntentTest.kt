/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.backgroundWriteAction
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.smartReadAction
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RunBlockingReleasingWriteIntentTest : ExplytJavaLightTestCase() {

    @OptIn(DelicateCoroutinesApi::class)
    fun _testToolReadIsNotBlockedByTheEdtWriteIntentWhileAWriteIsPending() {
        val application = ApplicationManager.getApplication()
        assertTrue("light tests run on the EDT", application.isDispatchThread)
        assertTrue("the EDT holds write-intent", application.isWriteIntentLockAcquired)

        val writer = GlobalScope.launch(Dispatchers.Default) { backgroundWriteAction { } }
        try {
            assertTrue(
                "a background write is pending behind the EDT's write-intent",
                awaitHoldingWriteIntent(5.seconds) { ApplicationManagerEx.getApplicationEx().isWriteActionPending },
            )

            val value = runBlockingReleasingWriteIntent(timeout = 30.seconds) {
                withTimeout(5.seconds) {
                    withContext(Dispatchers.IO) { smartReadAction(project) { 42 } }
                }
            }

            assertEquals(42, value)
            writer.joinWithin(10.seconds)
        } finally {
            if (writer.isActive) writer.cancel()
        }
    }

    private fun awaitHoldingWriteIntent(deadline: Duration, condition: () -> Boolean): Boolean {
        val until = System.nanoTime() + deadline.inWholeNanoseconds
        while (!condition()) {
            if (System.nanoTime() > until) return false
            Thread.sleep(10)
        }
        return true
    }

    private fun Job.joinWithin(deadline: Duration) =
        runBlocking(Dispatchers.Default) { withTimeout(deadline) { join() } }
}
