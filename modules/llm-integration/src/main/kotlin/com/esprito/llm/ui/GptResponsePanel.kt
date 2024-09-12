package com.esprito.llm.ui

import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import java.awt.Font

class GptResponsePanel : HtmlPanel() {
    private var message = ""

    @Nls
    override fun getBody(): String {
        return message
    }

    override fun getBodyFont(): Font {
        return UIUtil.getLabelFont()
    }

    fun updateMessage(updateMessage: String?) {
        this.message = updateMessage ?: ""
        update()
    }
}
