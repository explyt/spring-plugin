/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("UNUSED_PARAMETER")
fun <T> runBlockingReleasingWriteIntent(
    timeout: Duration = 60.seconds,
    block: suspend CoroutineScope.() -> T,
): T = runBlocking(block = block)
