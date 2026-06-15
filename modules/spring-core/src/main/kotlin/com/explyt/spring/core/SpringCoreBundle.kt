/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core

import com.intellij.AbstractBundle

private const val BUNDLE = "messages.SpringCoreBundle"

object SpringCoreBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: String, vararg params: Any): String =
        getMessage(key, *params)
}