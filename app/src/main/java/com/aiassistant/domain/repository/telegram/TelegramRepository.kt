package com.aiassistant.domain.repository.telegram

import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.model.TelegramUpdate
import kotlinx.coroutines.flow.Flow

interface TelegramRepository {
    suspend fun pollUpdates(offset: Long?): Result<List<TelegramUpdate>>
    suspend fun sendResponse(chatId: Long, text: String): Result<Unit>
    suspend fun getConversationHistory(chatId: Long, limit: Int): List<ChatMessage>
    suspend fun saveMessage(chatId: Long, content: String, isFromUser: Boolean)
    suspend fun validateToken(): Boolean
    fun observeConversations(): Flow<List<TelegramConversation>>
}

data class TelegramConversation(
    val chatId: Long,
    val username: String?,
    val firstName: String?,
    val lastMessageAt: Long
)
