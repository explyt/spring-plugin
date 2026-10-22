/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test.util

import com.intellij.diagnostic.ThreadDumper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.DebugAttachDetectorArgs
import com.intellij.testFramework.common.timeoutRunBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <T> runBlockingReleasingWriteIntent(
    timeout: Duration = 60.seconds,
    block: suspend CoroutineScope.() -> T,
): T =
    if (ApplicationManager.getApplication().isDispatchThread) runOnEdtReleasingWriteIntent(timeout, block)
    else timeoutRunBlocking(timeout, action = block)

@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> runOnEdtReleasingWriteIntent(timeout: Duration, block: suspend CoroutineScope.() -> T): T {
    val scope = CoroutineScope(Dispatchers.EDT + ModalityState.defaultModalityState().asContextElement())
    val deferred = scope.async(block = block)
    try {
        (ApplicationManager.getApplication() as ApplicationImpl).threadingSupport
            .releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
            PlatformTestUtil.waitWithEventsDispatching(
                { timeoutMessage(timeout, deferred) },
                { deferred.isCompleted },
                if (DebugAttachDetectorArgs.isAttached()) Int.MAX_VALUE else timeout.inWholeSeconds.toInt().coerceAtLeast(1),
            )
        }
    } finally {
        if (!deferred.isCompleted) scope.cancel()
    }
    return deferred.getCompleted()
}

private fun timeoutMessage(timeout: Duration, deferred: Deferred<*>): String {
    val dumpFile = PathManager.getLogDir()
        .createDirectories()
        .resolve("threadDump-runBlockingReleasingWriteIntent-${System.currentTimeMillis()}.log")
    dumpFile.writeText(ThreadDumper.getThreadDumpInfo(ThreadDumper.getThreadInfos(), false).rawDump)
    return "The block did not complete in $timeout ($deferred), thread dump: file:${dumpFile.toAbsolutePath()}"
}
