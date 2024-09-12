package com.esprito.llm.ui

import com.esprito.llm.model.LlmMessage
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.openapi.roots.ui.componentsList.layout.VerticalStackLayout
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel

class ChatMessageScrollablePanel(val project: Project) : ScrollablePanel(VerticalStackLayout()) {

    fun addMessageBlockWrapper(): JPanel {
        val messageWrapper = JPanel()
        messageWrapper.layout = BoxLayout(messageWrapper, BoxLayout.PAGE_AXIS)
        add(messageWrapper)
        return messageWrapper
    }

    fun addChatUserMessages(message: LlmMessage, threadPanel: JPanel) {
        message.userText?.let {
            threadPanel.add(ChatMessagePanel(it, showUser = true))
        }
        message.selectedText?.let {
            threadPanel.add(ChatMessagePanel(it, showUser = false))
        }
        if (message.files.isNotEmpty()) {
            threadPanel.add(SelectedFilesAccordion(project, message.files), BorderLayout.CENTER)
        }
    }

    private fun clearAll() {
        removeAll()
        update()
    }

    fun update() {
        repaint()
        revalidate()
    }
}