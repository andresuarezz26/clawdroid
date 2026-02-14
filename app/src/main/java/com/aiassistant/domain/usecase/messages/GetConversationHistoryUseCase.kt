package com.aiassistant.domain.usecase.messages

import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.repository.messages.MessagesRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetConversationHistoryUseCase @Inject constructor(
    private val repository: MessagesRepository
) {
    suspend operator fun invoke(chatId: Long, limit: Int = 20): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
             repository.getConversationHistory(chatId, limit)
        }
    }
}