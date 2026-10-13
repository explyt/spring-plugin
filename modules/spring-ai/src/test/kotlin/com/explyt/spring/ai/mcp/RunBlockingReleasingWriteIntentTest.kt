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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RunBlockingReleasingWriteIntentTest : ExplytJavaLightTestCase() {

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    fun testToolReadIsNotBlockedByTheEdtWriteIntentWhileAWriteIsPending() {
        val application = ApplicationManager.getApplication()
        assertTrue("light tests run on the EDT", application.isDispatchThread)
        assertTrue("the EDT holds write-intent", application.isWriteIntentLockAcquired)

        val value = runBlockingReleasingWriteIntent(timeout = 10.seconds) {
            val writer = GlobalScope.launch(Dispatchers.Default) { backgroundWriteAction { } }
            try {
                val writePending = withTimeoutOrNull(3.seconds) {
                    while (!ApplicationManagerEx.getApplicationEx().isWriteActionPending) delay(10.milliseconds)
                    true
                } ?: false
                assertTrue("a background write is pending behind the EDT's write-intent", writePending)

                val read = withTimeout(5.seconds) {
                    withContext(Dispatchers.IO) { smartReadAction(project) { 42 } }
                }
                withTimeout(5.seconds) { writer.join() }
                read
            } finally {
                if (writer.isActive) writer.cancel()
            }
        }

        assertEquals(42, value)
    }
}
