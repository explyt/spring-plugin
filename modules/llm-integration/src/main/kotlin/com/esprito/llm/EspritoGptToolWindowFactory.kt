package com.esprito.llm

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import javax.swing.JComponent

class EspritoGptToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatToolWindowPanel = EspritoChatGptTabbedPanel(project)
        val chatHistoryPanel = ChatHistoryPanel(project)

        addContent(toolWindow, chatToolWindowPanel, LlmBundle.message("esprito.gpt.chat"))
        addContent(toolWindow, chatHistoryPanel, LlmBundle.message("esprito.gpt.history"))

        addContentManagerListener(toolWindow, chatHistoryPanel)
    }

    private fun addContentManagerListener(
        toolWindow: ToolWindow,
        chatHistoryPanel: ChatHistoryPanel
    ) {
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                val content = event.content
                if ("History" == content.tabName && content.isSelected) {
                    chatHistoryPanel.updateHistory()
                }
            }
        })
    }

    private fun addContent(toolWindow: ToolWindow, panel: JComponent, displayName: String) {
        val contentManager = toolWindow.contentManager
        contentManager.addContent(contentManager.factory.createContent(panel, displayName, false))
    }
}