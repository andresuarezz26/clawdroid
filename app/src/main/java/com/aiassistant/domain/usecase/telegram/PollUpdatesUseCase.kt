package com.aiassistant.domain.usecase.telegram

import com.aiassistant.domain.model.TelegramUpdate
import com.aiassistant.domain.repository.telegram.TelegramRepository
import javax.inject.Inject

class PollUpdatesUseCase @Inject constructor(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(offset: Long?): Result<List<TelegramUpdate>> {
        return repository.pollUpdates(offset)
    }
}
