/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.statistic

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind

object StatisticUtil {

    fun Editor?.registerActionUsage(fixKey: StatisticActionId, previewKey: StatisticActionId?) {
        if (this?.editorKind == EditorKind.MAIN_EDITOR) {
            StatisticService.getInstance().addActionUsage(fixKey)
        } else if (previewKey != null) {
            StatisticService.getInstance().addActionUsage(previewKey)
        }
    }

}