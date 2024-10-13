package com.esprito.llm.service

import com.esprito.llm.model.LlmChat
import com.esprito.llm.model.LlmMessage
import com.explyt.ai.backend.http.MessageType.ASSISTANT
import com.explyt.ai.backend.http.MessageType.USER
import org.junit.Assert.assertEquals
import org.junit.Test

class LlmHttpExplytServiceTest {

    @Test
    fun allContextMessages() {
        val chat = LlmChat()
        chat.messages.addAll(
            listOf(
                llmMessage("request1", "response1"),
                llmMessage("request2", "response2"),
                llmMessage("request3", "response3")
            )
        )
        val messages = LlmHttpExplytService.getAllContextMessages(chat)
        val typeList = messages.map { it.type }
        assertEquals(listOf(USER, ASSISTANT, USER, ASSISTANT, USER, ASSISTANT), typeList)
    }

    @Test
    fun allContextMessagesOnlyUser() {
        val chat = LlmChat()
        chat.messages.addAll(
            listOf(llmMessage("request1"), llmMessage("request2"), llmMessage("request3"))
        )
        val messages = LlmHttpExplytService.getAllContextMessages(chat)
        val typeList = messages.map { it.type }
        assertEquals(listOf(USER), typeList)
    }

    @Test
    fun allContextMessagesReducedUsers() {
        val chat = LlmChat()
        chat.messages.addAll(
            listOf(
                llmMessage("request1"), llmMessage("request2"),
                llmMessage("request3", "response3"),
                llmMessage("request4"), llmMessage("request5"), llmMessage("request6"),
                llmMessage("request7", "response7"),
                llmMessage("request8"),
                llmMessage("request9", "response9")
            )
        )
        val messages = LlmHttpExplytService.getAllContextMessages(chat)
        val typeList = messages.map { it.type }
        val contents = messages.map { it.content }
        assertEquals(listOf(USER, ASSISTANT, USER, ASSISTANT, USER, ASSISTANT), typeList)
        assertEquals(listOf("request3", "response3", "request7", "response7", "request9", "response9"), contents)
    }

    private fun llmMessage(request: String, response: String? = null): LlmMessage {
        val message = LlmMessage()
        message.userText = request
        message.userFinalPrompt = request
        message.response = response
        return message
    }

}