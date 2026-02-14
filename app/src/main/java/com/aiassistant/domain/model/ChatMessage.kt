package com.aiassistant.domain.model

import java.util.UUID

const val DEFAULT_CHAT_ID = 1L

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)