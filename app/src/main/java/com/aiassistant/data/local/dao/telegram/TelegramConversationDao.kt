package com.aiassistant.data.local.dao.telegram

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aiassistant.data.local.entity.telegram.TelegramConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelegramConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: TelegramConversationEntity)

    @Query("SELECT * FROM telegram_conversations WHERE chatId = :chatId")
    suspend fun getById(chatId: Long): TelegramConversationEntity?

    @Query("SELECT * FROM telegram_conversations ORDER BY lastMessageAt DESC")
    fun getAllConversations(): Flow<List<TelegramConversationEntity>>

    @Query("DELETE FROM telegram_conversations WHERE chatId = :chatId")
    suspend fun delete(chatId: Long)

    @Query("UPDATE telegram_conversations SET lastMessageAt = :timestamp WHERE chatId = :chatId")
    suspend fun updateLastMessageTime(chatId: Long, timestamp: Long)
}
