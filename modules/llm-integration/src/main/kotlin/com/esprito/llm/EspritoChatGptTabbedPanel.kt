package com.esprito.llm

import com.esprito.llm.LlmBundle.message
import com.esprito.llm.model.LlmChat
import com.esprito.llm.ui.ChatMainPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.AtomicInteger
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionListener
import java.util.*
import java.util.stream.IntStream
import javax.swing.*


class EspritoChatGptTabbedPanel(val project: Project) : SimpleToolWindowPanel(true) {
    private val jbTabbedPane = JBTabbedPane()
    private val chatCount = AtomicInteger()

    init {

        val addTabButton = JButton(AllIcons.General.Add)
        addTabButton.setBorderPainted(false);
        addTabButton.border = null
        addTabButton.setFont(JBUI.Fonts.smallFont())
        addTabButton.addActionListener { addTab() }

        //----notification container - files/images
        val notificationContainer = JPanel(BorderLayout())
        notificationContainer.layout = BoxLayout(notificationContainer, BoxLayout.PAGE_AXIS)
        //notificationContainer.add(selectedFilesNotification)
        //notificationContainer.add(imageFileAttachmentNotification)


        setContent(JBUI.Panels.simplePanel(jbTabbedPane).addToBottom(notificationContainer))
        jbTabbedPane.componentPopupMenu = createPopupMenu()
        addTab()

        //---action bar
        val actionToolbarPanel = JPanel(BorderLayout())
        actionToolbarPanel.add(addTabButton, BorderLayout.LINE_START)
        toolbar = actionToolbarPanel
    }

    fun selectTabById(uuid: UUID): Boolean {
        for (i in 0 until jbTabbedPane.tabCount) {
            val chatMainPanel = jbTabbedPane.getComponentAt(i) as? ChatMainPanel ?: continue
            if (chatMainPanel.getTabId() == uuid) {
                jbTabbedPane.selectedIndex = i
                return true
            }
        }
        return false
    }

    fun addTab(chat: LlmChat? = null) {
        val tabPanel = ChatMainPanel(project, chat)
        val tabCount = jbTabbedPane.tabCount
        val chatName = getChatName(tabCount)
        jbTabbedPane.insertTab(chatName, null, tabPanel, null, tabCount)
        jbTabbedPane.setTabComponentAt(tabCount, createCloseableTabButtonPanel(chatName))
        jbTabbedPane.selectedIndex = tabCount
    }


    private fun getChatName(tabCount: Int): String {
        //return if (tabCount > 0) "chat:$tabCount" else "chat"
        return "chat:${chatCount.incrementAndGet()}"
    }

    private fun createCloseableTabButtonPanel(title: String): JPanel {
        val closeIcon = AllIcons.Actions.Close
        val button = JButton(closeIcon)
        button.addActionListener {
            val parent = (it.source as JButton).parent
            val indexOfTabComponent = jbTabbedPane.indexOfTabComponent(parent)
            if (indexOfTabComponent >= 0) {
                jbTabbedPane.removeTabAt(indexOfTabComponent)
            }
        }
        button.preferredSize = Dimension(closeIcon.iconWidth, closeIcon.iconHeight)
        button.border = BorderFactory.createEmptyBorder()
        button.isContentAreaFilled = false
        button.toolTipText = "Close Chat"
        button.rolloverIcon = AllIcons.Actions.CloseHovered

        return JBUI.Panels.simplePanel(4, 0)
            .addToLeft(JBLabel(title))
            .addToRight(button)
            .andTransparent()
    }

    private fun createPopupMenu(): JPopupMenu {
        return object : JPopupMenu() {
            init {
                add(createPopupMenuItem(message("esprito.gpt.chat.tab.close")) {
                    val selectedIdx = jbTabbedPane.selectedIndex
                    if (selectedIdx > 0) {
                        jbTabbedPane.removeTabAt(selectedIdx)
                    }
                })
                add(createPopupMenuItem(message("esprito.gpt.chat.tab.close.others")) {
                    val tabCount = jbTabbedPane.tabCount
                    val idx = jbTabbedPane.selectedIndex
                    IntStream.range(0, tabCount).filter { it != idx }
                        .mapToObj { jbTabbedPane.getComponentAt(it) }.toList()
                        .forEach { jbTabbedPane.remove(it) }
                })
            }
        }
    }

    private fun createPopupMenuItem(label: String, listener: ActionListener): JBMenuItem {
        val menuItem = JBMenuItem(label)
        menuItem.addActionListener(listener)
        return menuItem
    }
}
