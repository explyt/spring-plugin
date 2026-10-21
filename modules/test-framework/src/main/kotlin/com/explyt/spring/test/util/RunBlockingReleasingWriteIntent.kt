/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test.util

import com.intellij.testFramework.common.timeoutRunBlocking
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <T> runBlockingReleasingWriteIntent(
    timeout: Duration = 60.seconds,
    block: suspend CoroutineScope.() -> T,
): T = timeoutRunBlocking(timeout, action = block)
