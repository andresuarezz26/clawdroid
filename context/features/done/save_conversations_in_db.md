Plan: Unify Conversations to a Single DB Conversation

Context

Currently, TelegramBotService and NotificationReactor save messages to the database using the Telegram chatId, but ChatViewModel does not persist messages at all — it only keeps them in-memory
via MutableStateFlow. The goal is to have all 3 channels share a single conversation in the database, and have ChatViewModel load/save from it.

There's also a bug: MessagesRepositoryImpl.saveMessage() calls conversationDao.updateLastMessageTime() (an UPDATE), but there's a foreign key on MessageEntity.chatId →
ConversationEntity.chatId. If the ConversationEntity doesn't exist yet, the message insert will fail. Currently this only works because TelegramRepositoryImpl.pollUpdates() creates the
ConversationEntity via upsert() before any messages are saved. With a fixed chatId, we must ensure the conversation entity exists.

Changes

1. Add a default chatId constant

File: app/src/main/java/com/aiassistant/domain/model/ChatMessage.kt (or a new constants file)

Add a companion constant:
const val DEFAULT_CHAT_ID = 1L

This will be the single chatId used across all channels.

2. Fix MessagesRepositoryImpl.saveMessage() to ensure conversation exists

File: app/src/main/java/com/aiassistant/data/repository/messages/MessagesRepositoryImpl.kt

Before inserting the message, upsert the ConversationEntity so the foreign key is satisfied:
override suspend fun saveMessage(chatId: Long, content: String, isFromUser: Boolean) {
// Ensure conversation exists (upsert with IGNORE to not overwrite existing)
conversationDao.upsert(ConversationEntity(
chatId = chatId,
username = null,
firstName = null,
lastMessageAt = System.currentTimeMillis()
))
val entity = MessageEntity(...)
messageDao.insert(entity)
conversationDao.updateLastMessageTime(chatId, System.currentTimeMillis())
}

Note: upsert uses OnConflictStrategy.REPLACE, so this will update lastMessageAt on the existing row. The subsequent updateLastMessageTime call is redundant but harmless — we can remove it.

3. Update ChatViewModel to save and restore conversations

File: app/src/main/java/com/aiassistant/presentation/chat/ChatViewModel.kt

- Inject SaveMessageUseCase and GetConversationHistoryUseCase
- On init, load conversation history from DB and populate _state.messages
- In executeCommand():
    - Save user message to DB before executing agent
    - Save agent response to DB after execution
    - Use DB history for historyForContext instead of in-memory state
- In ClearHistory: also clear from DB (or just clear in-memory, depending on preference)

4. Update TelegramBotService to use DEFAULT_CHAT_ID for DB operations

File: app/src/main/java/com/aiassistant/framework/telegram/TelegramBotService.kt

- Use DEFAULT_CHAT_ID when calling saveMessageUseCase and getConversationHistoryUseCase
- Keep using the real Telegram chatId for sendResponseTelegramUseCase (to send back to Telegram)
- Pass DEFAULT_CHAT_ID to notificationReactor.startReacting()

5. Update NotificationReactor to use DEFAULT_CHAT_ID for DB operations

File: app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt

- Use DEFAULT_CHAT_ID when calling saveMessageUseCase and getConversationHistoryUseCase
- Keep using the real chatId parameter for sendResponseTelegramUseCase

6. Store the Telegram chatId separately for sending responses

File: app/src/main/java/com/aiassistant/framework/telegram/TelegramBotService.kt

Since we decouple DB chatId from Telegram chatId, the TelegramBotService needs to remember the real Telegram chatId for sending responses. It already has access to it from update.chatId in the
polling loop. The NotificationReactor.startReacting(chatId) parameter should become the Telegram chatId for sending, not the DB chatId.

Rename the parameter in NotificationReactor.startReacting() to telegramChatId for clarity.

Files to Modify

1. app/src/main/java/com/aiassistant/domain/model/ChatMessage.kt — add DEFAULT_CHAT_ID constant
2. app/src/main/java/com/aiassistant/data/repository/messages/MessagesRepositoryImpl.kt — ensure conversation exists on save
3. app/src/main/java/com/aiassistant/presentation/chat/ChatViewModel.kt — inject use cases, load/save from DB
4. app/src/main/java/com/aiassistant/framework/telegram/TelegramBotService.kt — use DEFAULT_CHAT_ID for DB
5. app/src/main/java/com/aiassistant/framework/notification/NotificationReactor.kt — use DEFAULT_CHAT_ID for DB, rename param

Verification

- Build the project with ./gradlew assembleDebug
- Verify no compilation errors
