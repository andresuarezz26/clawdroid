package com.aiassistant.domain.usecase.telegram

import com.aiassistant.domain.repository.telegram.TelegramRepository
import javax.inject.Inject

class SendResponseUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long, text: String): Result<Unit> {
        return repository.sendResponse(chatId, text)
    }
}
