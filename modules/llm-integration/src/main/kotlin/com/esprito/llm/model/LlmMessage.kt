package com.esprito.llm.model

import java.time.LocalDateTime
import java.util.*

class LlmMessage {
    var id = UUID.randomUUID()
    var created = LocalDateTime.now().toString()

    var userText: String? = null
    var selectedText: String? = null
    var response: String? = null
    var userFinalPrompt: String? = null

    var files: MutableList<String> = mutableListOf()
}