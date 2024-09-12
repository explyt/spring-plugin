package com.esprito.llm.ui;

import com.esprito.llm.LlmBundle
import com.esprito.llm.model.LlmChat
import com.esprito.llm.ui.listener.SendListener
import com.intellij.find.SearchTextArea
import com.intellij.icons.AllIcons
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JProgressBar

class SendMessagePanel(
    val project: Project,
    val messageScrollablePanel: ChatMessageScrollablePanel,
    val attachFilePanel: AttachFilePanel,
    val chat: LlmChat
) : JPanel(BorderLayout()) {
    val progressBar = JProgressBar()
    val jbTextArea = JBTextArea()
    val button = JButton(LlmBundle.message("esprito.gpt.chat.button.send"), AllIcons.Actions.Play_forward)

    init {
        val sendListener = SendListener(this)

        button.addActionListener(sendListener)
        button.setUI(DarculaButtonUI())

        jbTextArea.emptyText.text = LlmBundle.message("esprito.gpt.chat.placeholder")
        val searchTextArea = SearchTextArea(jbTextArea, false)
        searchTextArea.textArea.addKeyListener(sendListener)
        searchTextArea.minimumSize = Dimension(searchTextArea.width, 500)
        searchTextArea.setMultilineEnabled(true)

        progressBar.isVisible = false
        add(progressBar, BorderLayout.NORTH)
        add(searchTextArea, BorderLayout.CENTER)
        add(button, BorderLayout.EAST)
    }
}
