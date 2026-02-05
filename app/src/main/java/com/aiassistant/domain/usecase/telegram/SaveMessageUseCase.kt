package com.aiassistant.domain.usecase.telegram

import com.aiassistant.domain.repository.telegram.TelegramRepository
import javax.inject.Inject

class SaveMessageUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long, content: String, isFromUser: Boolean) {
        repository.saveMessage(chatId, content, isFromUser)
    }
}
