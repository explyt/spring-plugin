package com.esprito.llm.ui

import com.esprito.llm.model.LlmChat
import com.esprito.llm.util.GptUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.ScrollPaneFactory
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.util.*
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants

class ChatMainPanel(val project: Project, existsChat: LlmChat?) : OnePixelSplitter(true, .98f) {
    private val chat = existsChat ?: LlmChat()

    init {
        setDividerWidth(2)

        var gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 1.0
        gbc.weightx = 1.0
        gbc.gridx = 0
        gbc.gridy = 0

        val messageScrollablePanel = ChatMessageScrollablePanel(project)
        val chatPanel = JPanel(GridBagLayout())
        chatPanel.add(createScrollPaneWithSmartScroller(messageScrollablePanel), gbc)

        gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weighty = 0.0
        gbc.weightx = 1.0
        gbc.gridx = 0
        gbc.gridy = 1


        val attachFilePanel = AttachFilePanel(project)
        chatPanel.add(attachFilePanel, gbc)

        showExistedMessages(messageScrollablePanel)

        //landing or from history
        // messageScrollablePanel.displayLandingView(getLandingView())

        firstComponent = chatPanel
        secondComponent = SendMessagePanel(project, messageScrollablePanel, attachFilePanel, chat)

        setDropTarget(object : DropTarget() {
            @Synchronized
            override fun drop(evt: DropTargetDropEvent) {
                dragAndDropEventListener(evt, attachFilePanel)
            }
        })
    }

    private fun dragAndDropEventListener(evt: DropTargetDropEvent, attachFilePanel: AttachFilePanel) {
        try {
            evt.acceptDrop(DnDConstants.ACTION_COPY)
            val transferData = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor)
            val droppedFiles: List<File> = (transferData as? List<*>)
                ?.filterIsInstance<File>() ?: emptyList()

            attachFilePanel.addFiles(droppedFiles.filter { !it.isDirectory }.map { it.toPath() })
            attachFilePanel.showFiles()
            evt.dropComplete(true)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun showExistedMessages(messageScrollablePanel: ChatMessageScrollablePanel) {
        for (message in chat.messages) {
            val threadPanel = messageScrollablePanel.addMessageBlockWrapper()
            messageScrollablePanel.addChatUserMessages(message, threadPanel)
            GptUtil.splitMdCodeBlocks(message.response).forEachIndexed { index, s ->
                threadPanel.add(ChatMessagePanel(s, false, index == 0))
            }
        }
    }

    fun getTabId(): UUID = chat.id

    private fun createScrollPaneWithSmartScroller(scrollablePanel: ScrollablePanel): JScrollPane {
        val scrollPane = ScrollPaneFactory.createScrollPane(scrollablePanel, true)
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        SmartScroller(scrollPane);
        return scrollPane
    }
}