/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.provider

import com.explyt.spring.ai.action.ConvertCurlToHttpAction
import com.explyt.spring.ai.action.ConvertPostmanToHttpAction
import com.explyt.spring.web.httpclient.AdditionalHttpPanelLoader
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

class HttpAiActionPanelLoader : AdditionalHttpPanelLoader {
    override fun getPanel(): DialogPanel {
        return panel {
            row {
                button("Import from Postman", ConvertPostmanToHttpAction())
                button("From Curl", ConvertCurlToHttpAction())
            }
        }
    }
}