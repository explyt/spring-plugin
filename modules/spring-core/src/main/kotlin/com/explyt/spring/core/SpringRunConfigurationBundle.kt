/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core

import com.intellij.AbstractBundle

private const val BUNDLE = "messages.SpringRunConfigurationBundle"

object SpringRunConfigurationBundle : AbstractBundle(BUNDLE) {

    @JvmStatic
    fun message(key: String, vararg params: Any): String =
        getMessage(key, *params)

    fun plural(key: String, amount: Int, vararg params: Any): String {
        return if (amount == 1) {
            message("$key.one", *params)
        } else {
            message("$key.many", *params)
        }
    }
}