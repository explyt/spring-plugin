package com.esprito.llm.ui.listener

import com.esprito.llm.LlmBundle
import com.esprito.llm.model.LlmChat
import com.esprito.llm.model.LlmMessage
import com.esprito.llm.service.ChatsService
import com.esprito.llm.service.LlmHttpExplytService
import com.esprito.llm.ui.AttachedFiles
import com.esprito.llm.ui.ChatMessagePanel
import com.esprito.llm.ui.SendMessagePanel
import com.esprito.llm.util.GptUtil
import com.explyt.ai.backend.http.ChatResponse
import kotlinx.coroutines.Deferred
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.io.path.absolutePathString


class SendListener(private val sendMessagePanel: SendMessagePanel) : ActionListener, KeyListener {
    private val chat = sendMessagePanel.chat

    @Volatile
    private var requestAsyncDeferrd: Deferred<ChatResponse>? = null

    override fun actionPerformed(e: ActionEvent?) {
        actionPerformed()
    }

    private fun actionPerformed() {
        if (requestAsyncDeferrd != null) {
            requestAsyncDeferrd?.cancel()
            return
        }
        val editTextMessage = sendMessagePanel.jbTextArea.text?.takeIf { it.isNotEmpty() } ?: return
        val selectedFiles = sendMessagePanel.attachFilePanel.getFiles()
        val llmMessage = createMessage(chat, editTextMessage, selectedFiles)
        val threadPanel = sendMessagePanel.messageScrollablePanel.addMessageBlockWrapper()
        sendMessagePanel.messageScrollablePanel.addChatUserMessages(llmMessage, threadPanel)

        resetSendPanel()

        performRequest(llmMessage, selectedFiles, threadPanel)
    }

    private fun resetSendPanel() {
        sendMessagePanel.jbTextArea.text = ""
        sendMessagePanel.progressBar.setIndeterminate(true)
        sendMessagePanel.progressBar.isVisible = true
        sendMessagePanel.attachFilePanel.clearAll()
        sendMessagePanel.button.text = LlmBundle.message("esprito.gpt.chat.button.stop")
    }

    private fun createMessage(chat: LlmChat, text: String, selectedFiles: AttachedFiles): LlmMessage {
        val llmMessage = LlmMessage()
        llmMessage.userText = text
        llmMessage.selectedText = GptUtil.getSelectedEditorTextAndReset(sendMessagePanel.project)
        llmMessage.files += selectedFiles.allFiles().map { it.absolutePathString() }

        ChatsService.getInstance().addChatMessage(chat, llmMessage)
        return llmMessage
    }

    private fun performRequest(message: LlmMessage, selectedFiles: AttachedFiles, threadPanel: JPanel) {
        val finalPrompt = getFinalPrompt(selectedFiles, message) ?: return
        message.userFinalPrompt = finalPrompt
        val imagePath = selectedFiles.imageFile

        requestAsyncDeferrd = LlmHttpExplytService.getInstance().perform(chat)
        requestAsyncDeferrd?.invokeOnCompletion { throwable ->
            if (throwable != null) {
                throwable.printStackTrace()
                revertStateToSend()
                onError(threadPanel, throwable.localizedMessage)
            } else {
                val response = requestAsyncDeferrd?.getCompleted()?.response ?: return@invokeOnCompletion
                onComplete(response, message, threadPanel)
            }
        }
        /* requestAsyncDeferrd = LlmHttpService.getInstance().freeRequestAsync(finalPrompt, imagePath, object :
             CompletionEventListener<String> {
             override fun onComplete(messageBuilder: StringBuilder?) {
                 onComplete(messageBuilder?.toString(), message, threadPanel)
             }

             override fun onError(error: ErrorDetails, ex: Throwable) {
                 revertStateToSend()
                 val errorMessage = error.message ?: ex.message ?: return
                 onError(threadPanel, errorMessage)
             }

             override fun onCancelled(messageBuilder: java.lang.StringBuilder?) {
                 revertStateToSend()
             }
         })*/
    }

    private fun onComplete(
        text: String?, message: LlmMessage, threadPanel: JPanel
    ) {
        revertStateToSend()
        text ?: return
        ChatsService.getInstance().updateChatMessage(chat.id, message.id) { it.response = text }

        val markdownBlocks = GptUtil.splitMdCodeBlocks(text)
        SwingUtilities.invokeLater {
            markdownBlocks.forEachIndexed { index, s ->
                threadPanel.add(ChatMessagePanel(s, false, index == 0))
            }
            sendMessagePanel.messageScrollablePanel.update()
        }
    }

    private fun onError(threadPanel: JPanel, errorMessage: String) {
        SwingUtilities.invokeLater {
            threadPanel.add(ChatMessagePanel(errorMessage, false))
            sendMessagePanel.messageScrollablePanel.update()
        }
    }

    private fun getFinalPrompt(selectedFiles: AttachedFiles, message: LlmMessage): String? {
        return if (message.selectedText != null) {
            message.userText + message.selectedText
        } else if (selectedFiles.imageFile != null) {
            message.userText
        } else if (selectedFiles.textFiles.isNotEmpty() && message.userText != null) {
            GptUtil.getPromptWithSelectedFiles(message.userText!!, selectedFiles.textFiles)
        } else {
            message.userText
        }
    }

    private fun revertStateToSend() {
        requestAsyncDeferrd = null
        SwingUtilities.invokeLater {
            sendMessagePanel.button.text = LlmBundle.message("esprito.gpt.chat.button.send")
            sendMessagePanel.button.invalidate()

            sendMessagePanel.progressBar.setIndeterminate(false)
            sendMessagePanel.progressBar.isVisible = false
        }
    }

    override fun keyTyped(e: KeyEvent?) {}

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ENTER && !e.isControlDown && !e.isShiftDown) {
            e.consume()
            actionPerformed()
        }
    }

    override fun keyReleased(e: KeyEvent?) {}

}