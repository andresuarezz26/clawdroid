package com.aiassistant.domain.model

data class TelegramUpdate(
    val updateId: Long,
    val chatId: Long,
    val userId: Long,
    val username: String?,
    val firstName: String?,
    val text: String,
    val timestamp: Long
)
