package com.explyt.spring.web

import com.explyt.spring.web.view.EndpointsToolWindow
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory


class EndpointsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.helpId = "explyt.endpoints.tool.window"
        toolWindow.title = SpringWebBundle.message("explyt.web.endpoints.tool.window.title")

        val endpointsToolWindow = EndpointsToolWindow(project).apply {
            setup()
        }

        val content = toolWindow.contentManager.factory.createContent(endpointsToolWindow.component, "", false)
        content.isCloseable = false

        toolWindow.contentManager.addContent(content)
        toolWindow.contentManager.setSelectedContent(content, true)

        DumbService.getInstance(project).runWhenSmart {
            endpointsToolWindow.start()
        }
    }
}