package com.aiassistant.domain.usecase.telegram

import com.aiassistant.domain.repository.telegram.TelegramRepository
import javax.inject.Inject

class ValidateTokenUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(): Boolean {
        return repository.validateToken()
    }
}
