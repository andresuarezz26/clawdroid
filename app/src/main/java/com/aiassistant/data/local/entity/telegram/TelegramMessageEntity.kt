package com.aiassistant.data.local.entity.telegram

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "telegram_messages",
    foreignKeys = [
        ForeignKey(
            entity = TelegramConversationEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class TelegramMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatId: Long,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long
)
