package com.aiassistant.data.remote.telegram

import com.aiassistant.data.remote.telegram.model.Message
import com.aiassistant.data.remote.telegram.model.Update
import com.aiassistant.data.remote.telegram.model.User

interface TelegramApi {
    suspend fun getUpdates(offset: Long?, timeout: Int = 30): List<Update>
    suspend fun sendMessage(chatId: Long, text: String): Message
    suspend fun getMe(): User
}
