/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.quarkus.core

import com.intellij.AbstractBundle

private const val BUNDLE = "messages.QuarkusCoreBundle"

object QuarkusCoreBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: String, vararg params: Any): String =
        getMessage(key, *params)
}