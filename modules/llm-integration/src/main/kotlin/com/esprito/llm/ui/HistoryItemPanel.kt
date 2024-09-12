package com.esprito.llm.ui

import com.esprito.llm.ChatHistoryPanel
import com.esprito.llm.EspritoChatGptTabbedPanel
import com.esprito.llm.LlmBundle
import com.esprito.llm.model.LlmChat
import com.esprito.llm.service.ChatsService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JPanel
import javax.swing.SwingConstants


private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

class HistoryItemPanel(
    private val llmChat: LlmChat, private val chatHistoryPanel: ChatHistoryPanel, private val project: Project
) : JPanel(BorderLayout()) {
    init {
        val removeButton = ActionLink(LlmBundle.message("esprito.gpt.history.remove")) {
            ChatsService.getInstance().remove(llmChat.id)
            chatHistoryPanel.updateHistory()
        }
        removeButton.setFont(JBUI.Fonts.smallFont())
        val openButton = ActionLink(LlmBundle.message("esprito.gpt.history.open")) { openTabAction() }
        openButton.setFont(JBUI.Fonts.smallFont())

        val headerPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        headerPanel.add(createDisplayNameLabel(llmChat))
        headerPanel.add(openButton)
        headerPanel.add(removeButton)


        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0),
            JBUI.Borders.empty(12, 8, 8, 8)
        )
        add(headerPanel, BorderLayout.NORTH)
        llmChat.messages.firstOrNull()?.userText?.let {
            add(JBLabel(it), BorderLayout.SOUTH)
        }
    }

    private fun openTabAction() {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("EspritoGPT") ?: return
        toolWindow.show()

        val chatContent = toolWindow.contentManager.contents
            .firstOrNull { it.component is EspritoChatGptTabbedPanel } ?: return
        chatContent.let { toolWindow.contentManager.setSelectedContent(it) }

        val chatGptTabbedPanel = chatContent.component as EspritoChatGptTabbedPanel
        val isSelected = chatGptTabbedPanel.selectTabById(llmChat.id)
        if (!isSelected) {
            chatGptTabbedPanel.addTab(llmChat)
        }
    }

    private fun createDisplayNameLabel(llmChat: LlmChat): JBLabel {
        val format = DATE_TIME_FORMATTER.format(LocalDateTime.parse(llmChat.created))
        return JBLabel(format, SwingConstants.LEADING)
    }
}