/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.openapi.actionSystem.AnActionEvent

object ActionUtil {

    fun isEnabledAndVisible(e: AnActionEvent, isEnabledAndVisible: Boolean) {
        e.presentation.isEnabledAndVisible = isEnabledAndVisible
    }
}