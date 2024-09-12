package com.esprito.llm.model

import java.time.LocalDateTime
import java.util.*

class LlmChat {
    var id = UUID.randomUUID()
    var created = LocalDateTime.now().toString()
    var messages: MutableList<LlmMessage> = mutableListOf()
}