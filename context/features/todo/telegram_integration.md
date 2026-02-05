
---

# Telegram Bot Integration Plan for Clawdroid

## Goal

Allow external users to chat with the AI agent via Telegram. User installs Clawdroid, configures a bot token, and others can interact with the device agent through Telegram.

---

## Architecture Decisions

| Decision           | Choice                            | Rationale                                     |
| ------------------ | --------------------------------- | --------------------------------------------- |
| Telegram API       | Direct Ktor (no library)          | Already configured, simple REST, avoid bloat  |
| Service Type       | Foreground Service + Long Polling | Mobile IPs change, need persistent connection |
| Token Storage      | EncryptedSharedPreferences        | Follow existing ApiKeyModule pattern          |
| Conversation State | Room Database                     | Already configured, survives restarts         |
| Agent Routing      | Shared AgentExecutor class        | Reuse logic between UI and Telegram           |

---

## Minimal User Setup

1. Create bot via **@BotFather** in Telegram, copy token
   2. Open **Clawdroid → Settings → Telegram → Paste token**
   3. Enable **"Start Bot"**
   4. Find bot in Telegram and start chatting

---

## Implementation Plan

---

### Phase 1: Data Foundation

#### 1. Create Telegram DTOs (`data/remote/telegram/model/`)

* `TelegramResponse.kt`
  * `Update.kt`
  * `Message.kt`
  * `Chat.kt`
  * `User.kt`

Use `kotlinx.serialization` annotations.

---

#### 2. Create Database Entities (`data/local/entity/telegram`)

```kotlin
// TelegramConversationEntity.kt
@Entity(tableName = "telegram_conversations")
data class TelegramConversationEntity(
    @PrimaryKey val chatId: Long,
    val username: String?,
    val firstName: String?,
    val lastMessageAt: Long
)

// TelegramMessageEntity.kt
@Entity(tableName = "telegram_messages")
data class TelegramMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long
)
```

---

#### 3. Create DAOs (`data/local/dao/telegram`)

* `TelegramConversationDao.kt` → CRUD for conversations
  * `TelegramMessageDao.kt` → Message history queries

---

#### 4. Update AppDatabase (`data/local/AppDatabase.kt`)

* Add entities
  * Bump DB version to **2**
  * Add DAOs

---

### Phase 2: API & Repository Layer

#### 5. Create TelegramTokenProvider (`data/di/TelegramModule.kt`)

* Follow `BuildConfigApiKeyProvider` pattern exactly
  * Store token using key: `"telegram_bot_token"`

---

#### 6. Create TelegramApi (`data/remote/telegram/`)

```kotlin
interface TelegramApi {
    suspend fun getUpdates(offset: Long?, timeout: Int = 30): List<Update>
    suspend fun sendMessage(chatId: Long, text: String): Message
    suspend fun getMe(): User // For token validation
}
```

---

#### 7. Create TelegramApiImpl

* Use **Ktor HttpClient**
  * Timeout: **35 seconds** (Telegram polling is 30s)
  * Base URL:

```
https://api.telegram.org/bot{token}/
```

---

#### 8.1 Create TelegramRepository (`domain/repository/telegram`)

```kotlin
interface TelegramRepository {
    suspend fun pollUpdates(offset: Long?): Result<List<TelegramUpdate>>
    suspend fun sendResponse(chatId: Long, text: String): Result<Unit>
    suspend fun getConversationHistory(chatId: Long, limit: Int): List<ChatMessage>
    suspend fun saveMessage(chatId: Long, content: String, isFromUser: Boolean)
    suspend fun validateToken(): Boolean
}
```

#### 8.2 and Use Cases (`domain/usecase/telegram`)

```kotlin
class PollUpdatesFromTelegramUseCase @Inject constructor(val repository: TelegramRepository) {
    operator fun invoke(offset: Long?) : Flow<Result<List<TelegramUpdate>>>{ /* Placeholder for future cleanup */ }
}
```

Crete other ones: PollUpdatesTelegramUseCase.kt , SendResponseTelegramUseCase.kt, GetConversationHistoryTelegramUseCase.kt
, SaveMessageTelegramUseCase.kt, ValidateTokentTelegramUseCase.kt

---

### Phase 3: Agent Integration

#### 9. Create AgentExecutor (`agent/AgentExecutor.kt`)

```kotlin
@Singleton
class AgentExecutor @Inject constructor(
    private val agentFactory: AndroidAgentFactory,
    private val apiKeyProvider: ApiKeyProvider,
    private val screenRepository: ScreenRepository
) {
    suspend fun execute(
        command: String,
        conversationHistory: List<ChatMessage>
    ): AgentResult
}

sealed class AgentResult {
    data class Success(val response: String) : AgentResult()
    data class Failure(val reason: String) : AgentResult()
    data object ServiceNotConnected : AgentResult()
}
```

---

#### 10. Refactor ChatViewModel

* Use `AgentExecutor`
  * Reduce duplicated logic

---

### Phase 4: Service Layer

#### 11. Create TelegramBotService (`framework/telegram/`)

```kotlin
@AndroidEntryPoint
class TelegramBotService : Service()
```

Responsibilities:

* Foreground service with notification
  * Coroutine-based polling loop
  * Per-user message processing
  * Exponential backoff retry
  * It MUST depends on UseCases (DO NOT USE Repository Directly)

---

#### 12. Update AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<service
    android:name=".framework.telegram.TelegramBotService"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

---

#### 13. Create TelegramNotificationManager

* Notification channel setup
  * Status updates:

    * Running
    * Message count
    * Errors

---

### Phase 5: UI Layer

#### 14. Create TelegramSettingsViewModel (`presentation/settings/`)

* Token CRUD via `TelegramTokenProvider`
  * Service start/stop control
  * Connection test

---

#### 15. Create TelegramSettingsScreen

* Masked token input
  * **Test Connection** button
  * Start/Stop toggle
  * Status indicator
  * Active conversations list (optional)

---

#### 16. Add Navigation Entry

* Add settings button/link from main chat screen

---

## Critical Files to Modify

| File                              | Changes                          |
| --------------------------------- | -------------------------------- |
| `data/local/AppDatabase.kt`       | Add entities, DAOs, version bump |
| `AndroidManifest.xml`             | Add service and permission       |
| `data/di/NetworkModule.kt`        | Add HttpClient timeout config    |
| `presentation/chat/ChatScreen.kt` | Add settings entry point         |

---

## Files to Create

```
data/remote/telegram/
├── TelegramApi.kt
├── TelegramApiImpl.kt
└── model/
    ├── TelegramResponse.kt
    ├── Update.kt
    ├── Message.kt
    ├── Chat.kt
    └── User.kt

data/local/entity/telegram
├── TelegramConversationEntity.kt
└── TelegramMessageEntity.kt

data/local/dao/telegram
├── TelegramConversationDao.kt
└── TelegramMessageDao.kt

data/di/
└── TelegramModule.kt

domain/repository/telegram
└── TelegramRepository.kt

domain/usecase/telegram
└── PollUpdatesTelegramUseCase.kt
└── SendResponseTelegramUseCase.kt
└── GetConversationHistoryTelegramUseCase.kt
└── SaveMessageTelegramUseCase.kt
└── ValidateTokentTelegramUseCase.kt


data/repository/
└── TelegramRepositoryImpl.kt

agent/
├── AgentExecutor.kt
└── AgentResult.kt

framework/telegram/
├── TelegramBotService.kt
└── TelegramNotificationManager.kt

presentation/settings/
├── TelegramSettingsScreen.kt
└── TelegramSettingsViewModel.kt

```

---

## Verification Plan

### 1. Unit Tests

* TelegramApi parsing
  * Repository logic

### 2. Integration Test

* Configure token in app
  * Send message via Telegram
  * Verify agent processes and responds

### 3. Manual Test Scenarios

* Simple query → `"What time is it?"`
  * Device action → `"Open Settings"`
  * Multi-turn conversation
  * Service restart recovery
  * Network disconnection handling

---

## Error Handling

```kotlin
var retryDelay = 1_000L

while (isActive) {
    try {
        val updates = telegramRepository.pollUpdates(offset)
        retryDelay = 1_000L // Reset on success
        processUpdates(updates)
    } catch (e: Exception) {
        delay(retryDelay)
        retryDelay = (retryDelay * 2).coerceAtMost(30_000L)
    }
}
```

---

## Security Notes

* Token stored with **AES-256-GCM encryption**
  * Never log token values
  * Consider optional user allowlist (future enhancement)

---
