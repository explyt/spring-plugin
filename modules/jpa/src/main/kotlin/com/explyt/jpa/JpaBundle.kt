/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.JpaBundle"

object JpaBundle : DynamicBundle(BUNDLE) {

    @Nls
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any) =
        messageOrNull(key, *params) ?: ""
}