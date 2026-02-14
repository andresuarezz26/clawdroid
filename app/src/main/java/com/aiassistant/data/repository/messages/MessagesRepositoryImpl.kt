package com.aiassistant.data.repository.messages

import com.aiassistant.data.local.dao.telegram.ConversationDao
import com.aiassistant.data.local.dao.telegram.MessageDao
import com.aiassistant.data.local.entity.ConversationEntity
import com.aiassistant.data.local.entity.MessageEntity
import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.repository.messages.MessagesRepository
import com.aiassistant.domain.repository.telegram.ConversationModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessagesRepositoryImpl @Inject constructor(
  private val messageDao: MessageDao,
  private val conversationDao: ConversationDao,
) : MessagesRepository {

  override suspend fun getConversationHistory(chatId: Long, limit: Int): List<ChatMessage> {
    return messageDao.getRecentMessages(chatId, limit)
      .reversed()
      .map { entity ->
        ChatMessage(
          id = entity.id.toString(),
          content = entity.content,
          isUser = entity.isFromUser,
          timestamp = entity.timestamp
        )
      }
  }

  override suspend fun saveMessage(chatId: Long, content: String, isFromUser: Boolean) {
    val now = System.currentTimeMillis()
    // Ensure conversation exists (IGNORE so existing row + its messages are preserved)
    conversationDao.insertIfNotExists(ConversationEntity(
      chatId = chatId,
      username = null,
      firstName = null,
      lastMessageAt = now
    ))
    val entity = MessageEntity(
      chatId = chatId,
      content = content,
      isFromUser = isFromUser,
      timestamp = now
    )
    messageDao.insert(entity)
    conversationDao.updateLastMessageTime(chatId, now)
  }

  override fun observeConversations(): Flow<List<ConversationModel>> {
    return conversationDao.getAllConversations().map { entities ->
      entities.map { entity ->
        ConversationModel(
          chatId = entity.chatId,
          username = entity.username,
          firstName = entity.firstName,
          lastMessageAt = entity.lastMessageAt
        )
      }
    }
  }
}