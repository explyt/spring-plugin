package com.esprito.llm.service

import com.esprito.llm.model.LlmChat
import com.esprito.llm.model.LlmMessage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.util.*
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.APP)
class ChatsService {
    private var chats = mutableListOf<LlmChat>()
    private val chatLock = ReentrantLock()


    companion object {
        fun getInstance(): ChatsService = ApplicationManager.getApplication().service()
    }

    fun findChatByUUID(uuid: UUID): LlmChat? {
        try {
            chatLock.lock()
            return chats.find { it.id == uuid }
        } finally {
            chatLock.unlock()
        }

    }

    fun addChatMessage(chat: LlmChat, message: LlmMessage) {
        try {
            chatLock.lock()
            var findChat = chats.find { it.id == chat.id }
            if (findChat == null) {
                chats.add(chat)
                findChat = chat
            }
            findChat.messages.add(message)
        } finally {
            chatLock.unlock()
        }
    }

    fun updateChatMessage(chatId: UUID, messageId: UUID, supplier: (LlmMessage) -> Unit) {
        try {
            chatLock.lock()
            val chat = chats.find { it.id == chatId } ?: return
            val message = chat.messages.find { it.id == messageId } ?: return
            supplier.invoke(message)
        } finally {
            chatLock.unlock()
        }
    }

    fun getChats(): List<LlmChat> {
        try {
            chatLock.lock()
            return chats
        } finally {
            chatLock.unlock()
        }
    }

    fun removeAll() {
        try {
            chatLock.lock()
            chats.clear()
        } finally {
            chatLock.unlock()
        }
    }

    fun remove(id: UUID) {
        try {
            chatLock.lock()
            chats = chats.filter { it.id != id }.toMutableList()
        } finally {
            chatLock.unlock()
        }
    }
}