package com.aiassistant.data.local.entity.telegram

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_conversations")
data class TelegramConversationEntity(
    @PrimaryKey
    val chatId: Long,
    val username: String?,
    val firstName: String?,
    val lastMessageAt: Long
)
