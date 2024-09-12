package com.esprito.llm.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

class ChatMessagePanel(text: String, private val user: Boolean = true, showUser: Boolean = true) :
    JPanel(BorderLayout()) {
    init {
        val headerPanel = JPanel(BorderLayout())
        headerPanel.isOpaque = false
        if (showUser) {
            headerPanel.add(createDisplayNameLabel(), BorderLayout.LINE_START);
        }
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0),
            JBUI.Borders.empty(12, 8, 8, 8)
        )
        background = ColorUtil.brighter(background, if (user) 4 else 2)
        add(headerPanel, BorderLayout.NORTH)
        add(createResponseBody(text), BorderLayout.SOUTH)
    }

    private fun createDisplayNameLabel(): JBLabel {
        val userName = if (user) "Me" else "AI Assistant"
        val icon = if (user) AllIcons.General.User else AllIcons.Actions.QuickfixOffBulb
        return JBLabel(userName, icon, SwingConstants.LEADING)
            .setAllowAutoWrapping(true)
            .withFont(JBFont.label().asBold())
            .withBorder(JBUI.Borders.emptyBottom(6));
    }

    private fun createResponseBody(prompt: String): ChatMessageResponseBody {
        return ChatMessageResponseBody().withResponse(prompt)
    }
}