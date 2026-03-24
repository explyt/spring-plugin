/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.httpclient

import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.ui.DialogPanel

interface AdditionalHttpPanelLoader {
    fun getPanel(): DialogPanel

    companion object {
        val EP_NAME = ProjectExtensionPointName<AdditionalHttpPanelLoader>(
            "com.explyt.spring.web.additionalHttpPanelLoader"
        )
    }
}