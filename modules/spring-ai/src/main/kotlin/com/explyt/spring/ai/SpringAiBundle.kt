/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai

import com.intellij.AbstractBundle

private const val BUNDLE = "messages.SpringAiBundle"

object SpringAiBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: String, vararg params: Any): String = getMessage(key, *params)
}