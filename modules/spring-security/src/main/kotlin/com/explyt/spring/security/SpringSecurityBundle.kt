/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.security

import com.intellij.AbstractBundle

private const val BUNDLE = "messages.SpringSecurityBundle"

object SpringSecurityBundle : AbstractBundle(BUNDLE) {
    @JvmStatic
    fun message(key: String, vararg params: Any): String =
        getMessage(key, *params)
}