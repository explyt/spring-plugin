/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.util.OpenApiFileUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.TextEditorWithPreview

class RunInSwaggerAction(
    private val endpointInfos: List<AddEndpointToOpenApiIntention.EndpointInfo>,
    private val servers: List<String>
) : AnAction({ SpringWebBundle.message("explyt.web.run.linemarker.swagger.title") }, AllIcons.RunConfigurations.TestState.Run) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_OPENAPI_CONTROLLER_OPEN_IN_SWAGGER)

        OpenApiFileUtil.INSTANCE.createAndShow(
            project,
            endpointInfos,
            servers,
            TextEditorWithPreview.Layout.SHOW_PREVIEW
        )
    }

}