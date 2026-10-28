/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.util

fun String.lowercaseFirstChar(): String = replaceFirstChar { it.lowercaseChar() }
