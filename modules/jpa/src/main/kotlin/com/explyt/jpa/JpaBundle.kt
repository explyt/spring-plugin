/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls

private const val BUNDLE = "messages.JpaBundle"

object JpaBundle : DynamicBundle(BUNDLE) {

    @Nls
    fun message(key: String, vararg params: Any) =
        messageOrNull(key, *params) ?: ""
}