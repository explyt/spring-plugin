/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai

import com.intellij.openapi.util.IconLoader

object SpringAiIcons {
    private fun load(path: String) = IconLoader.getIcon(path, SpringAiIcons.javaClass)

    val aiAssistant = load("com/explyt/ai/icons/assistantResponseIcon.svg")
}