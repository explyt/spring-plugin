/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.core.util.BootWebApplicationType
import com.intellij.openapi.module.Module

enum class WebApplicationStack {
    SERVLET, REACTIVE;

    companion object {
        fun of(module: Module): WebApplicationStack? = when (BootWebApplicationType.cachedOf(module)) {
            BootWebApplicationType.SERVLET -> SERVLET
            BootWebApplicationType.REACTIVE -> REACTIVE
            BootWebApplicationType.NONE, BootWebApplicationType.UNKNOWN -> null
        }
    }
}
