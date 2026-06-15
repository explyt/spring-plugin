/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.initializr

import com.intellij.openapi.util.IconLoader

object SpringInitIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringInitIcons.javaClass)

    val Spring = load("com/explyt/spring/core/icons/spring.svg")

}