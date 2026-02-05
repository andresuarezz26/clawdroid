package com.aiassistant.data.local.dao.telegram

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aiassistant.data.local.entity.telegram.TelegramMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelegramMessageDao {

    @Insert
    suspend fun insert(message: TelegramMessageEntity)

    @Query("SELECT * FROM telegram_messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(chatId: Long, limit: Int): List<TelegramMessageEntity>

    @Query("SELECT * FROM telegram_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun observeMessages(chatId: Long): Flow<List<TelegramMessageEntity>>

    @Query("DELETE FROM telegram_messages WHERE chatId = :chatId")
    suspend fun deleteAllForChat(chatId: Long)

    @Query("SELECT COUNT(*) FROM telegram_messages WHERE chatId = :chatId")
    suspend fun getMessageCount(chatId: Long): Int
}
