package com.esprito.llm.ui

import com.esprito.llm.LlmBundle
import com.esprito.llm.util.GptUtil
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.lang.xml.XMLLanguage
import com.intellij.profile.codeInspection.ui.DescriptionEditorPane
import com.intellij.profile.codeInspection.ui.readHTMLWithCodeHighlighting
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.KotlinLanguage
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane

class ChatMessageResponseBody : JPanel(BorderLayout()) {
    init {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        isOpaque = false
    }

    fun withResponse(message: String): ChatMessageResponseBody {
        processResponse(message, message.startsWith("```"))
        return this
    }

    private fun processResponse(markdownInput: String, codeResponse: Boolean) {
        val htmlText = GptUtil.md2html(markdownInput)
        if (codeResponse) {
            val descriptionPanel = DescriptionEditorPane()
            val language = getLanguage(markdownInput)
            descriptionPanel.readHTMLWithCodeHighlighting(htmlText, language.id)

            val copyButton = copyButton(markdownInput)
            add(copyButton, BorderLayout.EAST)
            add(descriptionPanel)
        } else {
            add(createTextPane(htmlText))
        }
    }

    private fun copyButton(markdownInput: String): JComponent {
        val copyButton = ActionLink(LlmBundle.message("esprito.gpt.chat.copy")) {
            val stringSelection = StringSelection(
                markdownInput.substringAfter(System.lineSeparator()).replace("```", "")
            )
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(stringSelection, null)
        }
        copyButton.setFont(JBUI.Fonts.miniFont())
        val jPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        jPanel.add(copyButton)
        return jPanel
    }

    private fun getLanguage(markdownInput: String): Language {
        return if (markdownInput.startsWith("<?xml")) {
            XMLLanguage.INSTANCE
        } else if (markdownInput.contains("fun ")) {
            KotlinLanguage.INSTANCE
        } else {
            JavaLanguage.INSTANCE
        }
    }

    private fun createTextPane(text: String): JTextPane {
        val textPane = JTextPane()
        textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true)
        textPane.contentType = "text/html"
        textPane.isEditable = false
        textPane.text = text
        textPane.isOpaque = false
        textPane.setBorder(JBUI.Borders.empty())
        return textPane
    }

}