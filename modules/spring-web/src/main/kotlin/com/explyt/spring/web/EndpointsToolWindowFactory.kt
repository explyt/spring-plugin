/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web

import com.explyt.spring.web.view.EndpointsToolWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory


class EndpointsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.helpId = "explyt.endpoints.tool.window"
        toolWindow.title = SpringWebBundle.message("explyt.web.endpoints.tool.window.title")

        val endpointsToolWindow = EndpointsToolWindow(project)

        val content = toolWindow.contentManager.factory.createContent(endpointsToolWindow.component, "", false)
        content.isCloseable = true

        toolWindow.contentManager.addContent(content)
        toolWindow.contentManager.setSelectedContent(content, true)
    }
}