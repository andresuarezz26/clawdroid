package com.aiassistant.domain.usecase.telegram

import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.repository.telegram.TelegramRepository
import javax.inject.Inject

class GetConversationHistoryUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long, limit: Int = 20): List<ChatMessage> {
        return repository.getConversationHistory(chatId, limit)
    }
}
