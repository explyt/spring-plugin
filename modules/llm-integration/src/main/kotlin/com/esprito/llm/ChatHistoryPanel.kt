package com.esprito.llm

import com.esprito.llm.service.ChatsService
import com.esprito.llm.ui.HistoryItemPanel
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class ChatHistoryPanel(val project: Project) : SimpleToolWindowPanel(true) {
    private val scrollablePanel = ScrollablePanel()
    private val scrollPane = JBScrollPane()
    private val removeAllButton: ActionLink

    init {
        scrollablePanel.setLayout(BoxLayout(scrollablePanel, BoxLayout.Y_AXIS))

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        scrollPane.setViewportView(scrollablePanel)
        scrollPane.setBorder(null)
        scrollPane.setViewportBorder(null)


        setContent(scrollPane)

        val actionGroup = DefaultActionGroup("TOOLBAR_ACTION_GROUP", false)
        //actionGroup.add(MoveDownAction { this.refresh() })
        //actionGroup.add(MoveUpAction { this.refresh() })
        actionGroup.addSeparator()
        //actionGroup.add(DeleteAllConversationsAction { })

        removeAllButton = ActionLink(LlmBundle.message("esprito.gpt.history.remove.all")) {
            ChatsService.getInstance().removeAll()
            updateHistory()
        }
        removeAllButton.setFont(JBUI.Fonts.smallFont())


        val actionToolbarPanel = JPanel(BorderLayout())
        //actionToolbarPanel.add(toolbar.component, BorderLayout.LINE_START)
        actionToolbarPanel.add(removeAllButton, BorderLayout.LINE_END)
        toolbar = actionToolbarPanel

        updateHistory()
    }

    fun updateHistory() {
        scrollablePanel.removeAll()

        val chats = ChatsService.getInstance().getChats()
        removeAllButton.isVisible = chats.isNotEmpty()
        if (chats.isEmpty()) {
            val emptyLabel = JLabel(LlmBundle.message("esprito.gpt.history.empty"))
            emptyLabel.font = JBFont.h4()
            emptyLabel.border = JBUI.Borders.empty(8)
            scrollablePanel.add(emptyLabel)
        }
        chats.forEach { scrollablePanel.add(HistoryItemPanel(it, this, project)) }

        scrollablePanel.revalidate()
        scrollablePanel.repaint()
    }
}