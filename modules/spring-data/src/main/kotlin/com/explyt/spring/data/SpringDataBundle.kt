/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.data

import com.intellij.AbstractBundle

private const val BUNDLE = "messages.SpringDataBundle"

object SpringDataBundle : AbstractBundle(BUNDLE) {
    @JvmStatic
    fun message(key: String, vararg params: Any): String =
        getMessage(key, *params)
}