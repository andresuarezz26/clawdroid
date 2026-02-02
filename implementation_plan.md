# Implementation Plan - Clawdroid Android AI Automation App

**Status:** Implementation Started | **Last Updated:** February 2, 2026
**Architecture:** Single Module with Clean Architecture Packages

---

## 📊 Project Status Overview

### Completed Components

| Component                       | Status     | Notes                                   |
|---------------------------------|------------|-----------------------------------------|
| `ChatIntent`                    | ✅ Complete | Basic intents (UpdateInput, RunCommand) |
| `ChatState`                     | ✅ Complete | Basic state structure                   |
| `ChatViewModel`                 | 🟡 Partial | Basic structure, no logic yet           |
| `ChatScreen`                    | ✅ Complete | Basic UI structure                      |
| `HomeScreen`                    | ✅ Complete | Navigation setup                        |
| `MainActivity`                  | ✅ Complete | Basic setup                             |
| `AutomatorAccessibilityService` | 🟡 Partial | Basic service, no functionality         |
| Domain Layer                    | ❌ Missing  | No domain models or use cases           |
| Data Layer                      | ❌ Missing  | No repositories or mappers              |
| Framework Layer                 | 🟡 Partial | Accessibility service stub exists       |

### Current Architecture State

- **Single Module Structure:** All code in `:app` module (good!)
- **Basic UI Compose:** Chat/Home screens exist but no functionality
- **Navigation:** Basic NavHost setup between screens
- **Accessibility Service:** Service class exists but no screen reading or action execution
- **No LLM Integration:** No OpenAI API integration
- **No Dependency Injection:** Hilt is configured but no modules/providers

---

## 🎯 Implementation Plan

The plan maintains clean architecture principles through **package separation** within the single
`:app` module:

```
app/src/main/java/com/aiassistant/
├── domain/          # Pure Kotlin, zero Android dependencies
├── data/            # Repository implementations, LLM client
├── framework/       # Android-specific services (accessibility)
└── presentation/    # Compose UI, ViewModels
```

---

## Phase 1: Domain Layer (Week 1)

### 1.1 Domain Models

#### Step 1.2.1: Create domain/model/Bounds.kt

```kotlin
package com.aiassistant.domain.model

data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
```

**Estimated Time:** 2 minutes

#### Step 1.2.2: Create domain/model/UINode.kt

```kotlin
package com.aiassistant.domain.model

data class UINode(
    val index: Int,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isChecked: Boolean?,
    val bounds: Bounds,
    val children: List<UINode>
)
```

**Estimated Time:** 3 minutes

#### Step 1.2.3: Create domain/model/Action.kt

```kotlin
package com.aiassistant.domain.model

data class Action(
    val type: ActionType,
    val index: Int? = null,
    val packageName: String? = null,
    val text: String? = null,
    val direction: ScrollDirection? = null
)

enum class ActionType { LAUNCH, CLICK, SET_TEXT, SCROLL, BACK, HOME }
enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }
```

**Estimated Time:** 3 minutes

#### Step 1.2.4: Create domain/model/ActionResponse.kt

```kotlin
package com.aiassistant.domain.model

data class ActionResponse(
    val thought: String,
    val status: TaskStatus,
    val actions: List<Action>
)

enum class TaskStatus { ACTING, DONE, FAILED }
```

**Estimated Time:** 2 minutes

#### Step 1.2.5: Create domain/model/TaskResult.kt

```kotlin
package com.aiassistant.domain.model

sealed interface TaskResult {
    data class Success(val summary: String, val stepsUsed: Int) : TaskResult
    data class Failed(val reason: String, val stepsUsed: Int) : TaskResult
    data object MaxStepsReached : TaskResult
    data object Cancelled : TaskResult
}
```

**Estimated Time:** 3 minutes

#### Step 1.2.6: Create domain/model/StepResult.kt

```kotlin
package com.aiassistant.domain.model

data class StepResult(
    val stepNumber: Int,
    val thought: String,
    val actionsTaken: List<Action>,
    val status: TaskStatus
)
```

**Estimated Time:** 2 minutes

#### Step 1.2.7: Create domain/model/ChatMessage.kt

```kotlin
package com.aiassistant.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
```

**Estimated Time:** 2 minutes

#### Step 1.2.8: Create domain/model/AppTarget.kt

```kotlin
package com.aiassistant.domain.model

enum class AppTarget(val packageName: String, val displayName: String) {
    YOUTUBE("com.google.android.youtube", "YouTube"),
    LINKEDIN("com.linkedin.android", "LinkedIn"),
    TINDER("com.tinder", "Tinder"),
    INSTAGRAM("com.instagram.android", "Instagram"),
    WHATSAPP("com.whatsapp", "WhatsApp"),
    SPOTIFY("com.spotify.music", "Spotify"),
    CHROME("com.android.chrome", "Chrome"),
    GMAIL("com.google.android.gm", "Gmail"),
    MAPS("com.google.android.apps.maps", "Google Maps"),
    TWITTER("com.twitter.android", "X / Twitter"),
}
```

**Estimated Time:** 3 minutes

**Total Phase 1.2 Time:** ~20 minutes

---

### 1.3 Domain Repository Interfaces

#### Step 1.3.1: Create domain/repository/ScreenRepository.kt

```kotlin
package com.aiassistant.domain.repository

import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.UINode

interface ScreenRepository {
    suspend fun captureScreen(): List<UINode>
    suspend fun performAction(action: Action): Boolean
    fun isServiceConnected(): Boolean
}
```

**Estimated Time:** 2 minutes

#### Step 1.3.2: Create domain/repository/LlmRepository.kt

```kotlin
package com.aiassistant.domain.repository

import com.aiassistant.domain.model.ActionResponse
import com.aiassistant.domain.model.UINode

interface LlmRepository {
    suspend fun getNextActions(
        screenState: List<UINode>,
        userCommand: String,
        conversationHistory: List<Pair<String, String>>
    ): ActionResponse
}
```

**Estimated Time:** 2 minutes

#### Step 1.3.3: Create domain/repository/TaskLogRepository.kt

```kotlin
package com.aiassistant.domain.repository

import com.aiassistant.domain.model.StepResult
import com.aiassistant.domain.model.TaskResult
import kotlinx.coroutines.flow.Flow

interface TaskLogRepository {
    suspend fun saveLog(command: String, result: TaskResult, steps: List<StepResult>)
    fun getRecentLogs(limit: Int): Flow<List<TaskLog>>
}
```

**Estimated Time:** 2 minutes

**Total Phase 1.3 Time:** ~6 minutes

---

### 1.4 Domain Use Cases

#### Step 1.4.1: Create domain/usecase/ExecuteTaskUseCase.kt

```kotlin
package com.aiassistant.domain.usecase

import com.aiassistant.domain.model.*
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.repository.LlmRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ExecuteTaskUseCase @Inject constructor(
    private val screenRepo: ScreenRepository,
    private val llmRepo: LlmRepository
) {
    companion object {
        private const val MAX_STEPS = 20
        private const val SETTLE_DELAY_MS = 1500L
    }

    operator fun invoke(command: String): Flow<StepResult> = flow {
        val history = mutableListOf<Pair<String, String>>()
        var stepCount = 0

        while (stepCount < MAX_STEPS) {
            currentCoroutineContext().ensureActive()

            val screen = screenRepo.captureScreen()
            if (screen.isEmpty()) {
                emit(StepResult(stepCount, "Cannot read screen", emptyList(), TaskStatus.FAILED))
                return@flow
            }

            val response = llmRepo.getNextActions(screen, command, history)

            emit(StepResult(stepCount, response.thought, response.actions, response.status))

            if (response.status == TaskStatus.DONE || response.status == TaskStatus.FAILED) {
                return@flow
            }

            for (action in response.actions) {
                val success = screenRepo.performAction(action)
                if (!success) {
                    history.add("user" to "Action ${action.type} on index ${action.index} FAILED")
                }
            }

            delay(SETTLE_DELAY_MS)
            history.add("assistant" to response.thought)
            stepCount++
        }

        emit(StepResult(stepCount, "Maximum steps reached", emptyList(), TaskStatus.FAILED))
    }
}
```

**Estimated Time:** 10 minutes

#### Step 1.4.2: Create domain/usecase/CaptureScreenUseCase.kt

```kotlin
package com.aiassistant.domain.usecase

import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.model.UINode
import javax.inject.Inject

class CaptureScreenUseCase @Inject constructor(
    private val screenRepository: ScreenRepository
) {
    suspend operator fun invoke(): List<UINode> = screenRepository.captureScreen()
}
```

**Estimated Time:** 3 minutes

#### Step 1.4.3: Create domain/usecase/GetTaskHistoryUseCase.kt

```kotlin
package com.aiassistant.domain.usecase

import com.aiassistant.domain.repository.TaskLogRepository
import com.aiassistant.domain.model.TaskLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTaskHistoryUseCase @Inject constructor(
    private val taskLogRepository: TaskLogRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<TaskLog>> = 
        taskLogRepository.getRecentLogs(limit)
}
```

**Estimated Time:** 3 minutes

#### Step 1.4.4: Create domain/usecase/CancelTaskUseCase.kt

```kotlin
package com.aiassistant.domain.usecase

import javax.inject.Inject

class CancelTaskUseCase @Inject constructor() {
    operator fun invoke() { /* Placeholder for future cleanup */ }
}
```

**Estimated Time:** 1 minute

**Total Phase 1.4 Time:** ~17 minutes

---

## Phase 2: Data Layer Implementation (Week 2)

### 2.1 Data Mapper Components

#### Step 2.1.1: Create data/mapper/ScreenParser.kt

```kotlin
package com.aiassistant.data.mapper

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.aiassistant.domain.model.Bounds
import com.aiassistant.domain.model.UINode
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenParser @Inject constructor() {
    private val _nodeMap = ConcurrentHashMap<Int, AccessibilityNodeInfo>()
    val nodeMap: Map<Int, AccessibilityNodeInfo> = _nodeMap

    fun parse(rootNode: AccessibilityNodeInfo): List<UINode> {
        val nodeIndex = AtomicInteger(0)
        _nodeMap.clear()
        return walkTree(rootNode, nodeIndex)
    }

    private fun walkTree(
        node: AccessibilityNodeInfo,
        nodeIndex: AtomicInteger
    ): List<UINode> {
        if (!node.isVisibleToUser) return emptyList()

        val rect = Rect()
        node.getBoundsInScreen(rect)

        val children = mutableListOf<UINode>()
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { children.addAll(walkTree(it, nodeIndex)) }
        }

        val isRelevant = node.isClickable || node.isScrollable || node.isEditable ||
                !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()

        return if (isRelevant) {
            val idx = nodeIndex.getAndIncrement()
            _nodeMap[idx] = node
            listOf(UINode(
                index = idx,
                className = node.className?.toString() ?: "",
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                resourceId = node.viewIdResourceName,
                isClickable = node.isClickable,
                isScrollable = node.isScrollable,
                isEditable = node.isEditable,
                isChecked = if (node.isCheckable) node.isChecked else null,
                bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
                children = children
            ))
        } else children
    }
}
```

**Estimated Time:** 20 minutes

#### Step 2.1.2: Create data/mapper/UINodeFormatter.kt

```kotlin
package com.aiassistant.data.mapper

import com.aiassistant.domain.model.UINode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UINodeFormatter @Inject constructor() {
    fun format(nodes: List<UINode>): String {
        val sb = StringBuilder()
        flattenToText(nodes, sb)
        return sb.toString()
    }

    private fun flattenToText(nodes: List<UINode>, sb: StringBuilder) {
        for (node in nodes) {
            val props = buildList {
                if (node.isClickable) add("clickable")
                if (node.isScrollable) add("scrollable")
                if (node.isEditable) add("editable")
                node.isChecked?.let { add(if (it) "checked" else "unchecked") }
            }
            val label = node.text ?: node.contentDescription ?: node.resourceId ?: ""
            val b = node.bounds
            sb.appendLine(
                "[${node.index}] ${node.className.substringAfterLast('.')} " +
                "\"$label\" (${props.joinToString()}) [${b.left},${b.top},${b.right},${b.bottom}]"
            )
            flattenToText(node.children, sb)
        }
    }
}
```

**Estimated Time:** 10 minutes

#### Step 2.1.3: Create data/mapper/ActionResponseMapper.kt

```kotlin
package com.aiassistant.data.mapper

import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionResponse
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.model.TaskStatus
import com.aiassistant.data.remote.dto.ChatCompletionResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionResponseMapper @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun toDomain(response: ChatCompletionResponse): ActionResponse {
        val content = response.choices.first().message.content
        val raw = json.decodeFromString<RawActionResponse>(content)

        return ActionResponse(
            thought = raw.thought,
            status = when (raw.status) {
                "done" -> TaskStatus.DONE
                "failed" -> TaskStatus.FAILED
                else -> TaskStatus.ACTING
            },
            actions = raw.actions.map { mapAction(it) }
        )
    }

    private fun mapAction(raw: RawAction): Action {
        return Action(
            type = when (raw.type) {
                "launch" -> ActionType.LAUNCH
                "click" -> ActionType.CLICK
                "setText" -> ActionType.SET_TEXT
                "scroll" -> ActionType.SCROLL
                "back" -> ActionType.BACK
                "home" -> ActionType.HOME
                else -> throw IllegalArgumentException("Unknown action: ${raw.type}")
            },
            index = raw.index,
            packageName = raw.packageName,
            text = raw.text,
            direction = raw.direction?.let { ScrollDirection.valueOf(it.uppercase()) }
        )
    }
}
```

**Estimated Time:** 15 minutes

**Total Phase 2.1 Time:** ~45 minutes

---

### 2.2 Data Remote Components (LLM Integration)

#### Step 2.2.1: Create data/remote/OpenAIApiService.kt

```kotlin
package com.aiassistant.data.remote

import com.aiassistant.data.remote.dto.ChatCompletionRequest
import com.aiassistant.data.remote.dto.ChatCompletionResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAIApiService @Inject constructor(
    private val client: HttpClient
) {
    suspend fun createCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
        return client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
```

**Estimated Time:** 10 minutes

#### Step 2.2.2: Create data/remote/dto/ChatCompletionRequest.kt

```kotlin
package com.aiassistant.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<MessageDto>,
    @SerialName("response_format") val responseFormat: ResponseFormatDto,
    val temperature: Double
)

@Serializable
data class MessageDto(val role: String, val content: String)

@Serializable
data class ResponseFormatDto(val type: String)
```

**Estimated Time:** 5 minutes

#### Step 2.2.3: Create data/remote/dto/ChatCompletionResponse.kt

```kotlin
package com.aiassistant.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChoiceDto>
)

@Serializable
data class ChoiceDto(
    val message: MessageDto
)
```

**Estimated Time:** 3 minutes

#### Step 2.2.4: Create data/repository/SystemPrompt.kt (system prompt constant)

```kotlin
package com.aiassistant.data.repository

const val SYSTEM_PROMPT = """
You are an Android phone automation agent. You receive the current
screen state as a list of UI elements and a user command.

Your job: return a JSON object with the next action(s) to perform.

RULES:
- Return ONLY valid JSON, no explanation text
- Each action targets an element by its [index] number
- Perform ONE action at a time, then wait for new screen state
- If the task is complete, return {"status": "done", "actions": []}
- If you need to launch an app first, use the "launch" action
- If you cannot find the target element, use "scroll" to reveal more

ACTION TYPES:
- {"type": "launch", "package": "com.google.android.youtube"}
- {"type": "click", "index": 5}
- {"type": "setText", "index": 3, "text": "search query here"}
- {"type": "scroll", "index": 2, "direction": "down"}
- {"type": "back"}
- {"type": "home"}

KNOWN APP PACKAGES:
- YouTube: com.google.android.youtube
- LinkedIn: com.linkedin.android
- Tinder: com.tinder
- Instagram: com.instagram.android
- WhatsApp: com.whatsapp
- Spotify: com.spotify.music
- Chrome: com.android.chrome

RESPONSE FORMAT:
{
  "thought": "brief reasoning about what to do next",
  "status": "acting" | "done" | "failed",
  "actions": [{"type": "...", ...}]
}
"""
```

**Estimated Time:** 5 minutes

#### Step 2.2.5: Create data/di/NetworkModule.kt (Hilt module for Ktor)

```kotlin
package com.aiassistant.data.di

import com.aiassistant.data.remote.OpenAIApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    @Provides
    @Singleton
    fun provideOpenAIApiService(client: HttpClient): OpenAIApiService {
        return OpenAIApiService(client)
    }
}
```

**Estimated Time:** 10 minutes

**Total Phase 2.2 Time:** ~33 minutes

---

### 2.3 Data Repository Implementations

#### Step 2.3.1: Create data/repository/LlmRepositoryImpl.kt

```kotlin
package com.aiassistant.data.repository

import com.aiassistant.domain.model.ActionResponse
import com.aiassistant.domain.model.UINode
import com.aiassistant.domain.repository.LlmRepository
import com.aiassistant.data.mapper.UINodeFormatter
import com.aiassistant.data.mapper.ActionResponseMapper
import com.aiassistant.data.remote.OpenAIApiService
import com.aiassistant.data.remote.dto.ChatCompletionRequest
import com.aiassistant.data.remote.dto.MessageDto
import com.aiassistant.data.remote.dto.ResponseFormatDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val apiService: OpenAIApiService,
    private val formatter: UINodeFormatter,
    private val responseMapper: ActionResponseMapper
) : LlmRepository {

    override suspend fun getNextActions(
        screenState: List<UINode>,
        userCommand: String,
        conversationHistory: List<Pair<String, String>>
    ): ActionResponse {
        val screenText = formatter.format(screenState)

        val messages = buildList {
            add(MessageDto("system", SYSTEM_PROMPT))
            conversationHistory.forEach { (role, content) ->
                add(MessageDto(role, content))
            }
            add(MessageDto("user", "SCREEN STATE:\n$screenText\n\nUSER COMMAND: $userCommand"))
        }

        val request = ChatCompletionRequest(
            model = "gpt-4o",
            messages = messages,
            responseFormat = ResponseFormatDto(type = "json_object"),
            temperature = 0.1
        )

        val response = apiService.createCompletion(request)
        return responseMapper.toDomain(response)
    }
}
```

**Estimated Time:** 15 minutes

#### Step 2.3.2: Create data/repository/ScreenRepositoryImpl.kt

```kotlin
package com.aiassistant.data.repository

import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.UINode
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.data.mapper.ScreenParser
import com.aiassistant.framework.accessibility.AccessibilityServiceBridge
import com.aiassistant.framework.accessibility.ActionPerformer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenRepositoryImpl @Inject constructor(
    private val screenParser: ScreenParser,
    private val serviceBridge: AccessibilityServiceBridge,
    private val actionPerformer: ActionPerformer
) : ScreenRepository {

    override suspend fun captureScreen(): List<UINode> {
        val rootNode = serviceBridge.getRootNode() ?: return emptyList()
        return screenParser.parse(rootNode)
    }

    override suspend fun performAction(action: Action): Boolean {
        return actionPerformer.execute(action, screenParser.nodeMap)
    }

    override fun isServiceConnected(): Boolean {
        return serviceBridge.isConnected()
    }
}
```

**Estimated Time:** 10 minutes

#### Step 2.3.3: Create data/repository/TaskLogRepositoryImpl.kt

```kotlin
package com.aiassistant.data.repository

import com.aiassistant.data.local.dao.TaskLogDao
import com.aiassistant.data.local.entity.TaskLogEntity
import com.aiassistant.domain.model.*
import com.aiassistant.domain.repository.TaskLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskLogRepositoryImpl @Inject constructor(
    private val taskLogDao: TaskLogDao
) : TaskLogRepository {

    override suspend fun saveLog(command: String, result: TaskResult, steps: List<StepResult>) {
        val entity = TaskLogEntity(
            command = command,
            resultType = when (result) {
                is TaskResult.Success -> "success"
                is TaskResult.Failed -> "failed"
                is TaskResult.MaxStepsReached -> "max_steps"
                is TaskResult.Cancelled -> "cancelled"
            },
            resultSummary = when (result) {
                is TaskResult.Success -> result.summary
                is TaskResult.Failed -> result.reason
                else -> result.toString()
            },
            stepsUsed = steps.size,
            timestamp = System.currentTimeMillis()
        )
        taskLogDao.insert(entity)
    }

    override fun getRecentLogs(limit: Int): Flow<List<TaskLog>> {
        return taskLogDao.getRecent(limit).map { entities ->
            entities.map { entity ->
                TaskLog(
                    id = entity.id,
                    command = entity.command,
                    result = entity.resultType,
                    resultSummary = entity.resultSummary,
                    timestamp = entity.timestamp
                )
            }
        }
    }
}
```

**Estimated Time:** 15 minutes

**Total Phase 2.3 Time:** ~40 minutes

---

### 2.4 Data DI Module

#### Step 2.4.1: Create data/di/DataModule.kt

```kotlin
package com.aiassistant.data.di

import com.aiassistant.data.repository.LlmRepositoryImpl
import com.aiassistant.data.repository.ScreenRepositoryImpl
import com.aiassistant.data.repository.TaskLogRepositoryImpl
import com.aiassistant.domain.repository.LlmRepository
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.repository.TaskLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindScreenRepository(impl: ScreenRepositoryImpl): ScreenRepository

    @Binds
    @Singleton
    abstract fun bindLlmRepository(impl: LlmRepositoryImpl): LlmRepository

    @Binds
    @Singleton
    abstract fun bindTaskLogRepository(impl: TaskLogRepositoryImpl): TaskLogRepository
}
```

**Estimated Time:** 10 minutes

**Total Phase 2 Time:** ~128 minutes (2+ hours)

---

## Phase 3: Framework Layer (Week 2)

### 3.1 Accessibility Service Components

#### Step 3.1.1: Move AutomatorAccessibilityService to framework package
Move from `app/src/main/java/com/aiassistant/presentation/service/AutomatorAccessibilityService.kt`
to `app/src/main/java/com/aiassistant/framework/accessibility/AutomatorAccessibilityService.kt`

Update implementation:

```kotlin
package com.aiassistant.framework.accessibility

import android.accessibilityservice.AccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AutomatorAccessibilityService : AccessibilityService() {

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected
        private var instance: AutomatorAccessibilityService? = null

        fun getInstance(): AutomatorAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onDestroy() {
        instance = null
        _isConnected.value = false
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) { }
    override fun onInterrupt() { }
}
```

**Estimated Time:** 15 minutes

#### Step 3.1.2: Create framework/accessibility/AccessibilityServiceBridge.kt

```kotlin
package com.aiassistant.framework.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject
import javax.inject.Singleton

interface AccessibilityServiceBridge {
    fun getRootNode(): AccessibilityNodeInfo?
    fun isConnected(): Boolean
    fun performGlobalAction(action: Int): Boolean
    fun getContext(): Context?
}

@Singleton
class AccessibilityServiceBridgeImpl @Inject constructor() : AccessibilityServiceBridge {

    override fun getRootNode(): AccessibilityNodeInfo? {
        return AutomatorAccessibilityService.getInstance()?.rootInActiveWindow
    }

    override fun isConnected(): Boolean {
        return AutomatorAccessibilityService.isConnected.value
    }

    override fun performGlobalAction(action: Int): Boolean {
        return AutomatorAccessibilityService.getInstance()
            ?.performGlobalAction(action) ?: false
    }

    override fun getContext(): Context? {
        return AutomatorAccessibilityService.getInstance()
    }
}
```

**Estimated Time:** 10 minutes

#### Step 3.1.3: Create framework/accessibility/ActionPerformer.kt

```kotlin
package com.aiassistant.framework.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.ScrollDirection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionPerformer @Inject constructor(
    private val serviceBridge: AccessibilityServiceBridge
) {
    suspend fun execute(action: Action, nodeMap: Map<Int, AccessibilityNodeInfo>): Boolean {
        return when (action.type) {
            ActionType.LAUNCH -> launchApp(action.packageName!!)
            ActionType.CLICK -> clickNode(nodeMap, action.index!!)
            ActionType.SET_TEXT -> setTextOnNode(nodeMap, action.index!!, action.text!!)
            ActionType.SCROLL -> scrollNode(nodeMap, action.index!!, action.direction!!)
            ActionType.BACK -> serviceBridge.performGlobalAction(GLOBAL_ACTION_BACK)
            ActionType.HOME -> serviceBridge.performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun clickNode(nodeMap: Map<Int, AccessibilityNodeInfo>, index: Int): Boolean {
        val node = nodeMap[index] ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun setTextOnNode(
        nodeMap: Map<Int, AccessibilityNodeInfo>,
        index: Int,
        text: String
    ): Boolean {
        val node = nodeMap[index] ?: return false
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun scrollNode(
        nodeMap: Map<Int, AccessibilityNodeInfo>,
        index: Int,
        direction: ScrollDirection
    ): Boolean {
        val node = nodeMap[index] ?: return false
        val scrollAction = when (direction) {
            ScrollDirection.DOWN, ScrollDirection.RIGHT ->
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            ScrollDirection.UP, ScrollDirection.LEFT ->
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return node.performAction(scrollAction)
    }

    private fun launchApp(packageName: String): Boolean {
        val context = serviceBridge.getContext() ?: return false
        val intent = context.packageManager
            .getLaunchIntentForPackage(packageName)
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (intent != null) {
            context.startActivity(intent)
            true
        } else false
    }
}
```

**Estimated Time:** 20 minutes

#### Step 3.1.4: Create framework/di/FrameworkModule.kt

```kotlin
package com.aiassistant.framework.di

import com.aiassistant.framework.accessibility.AccessibilityServiceBridge
import com.aiassistant.framework.accessibility.AccessibilityServiceBridgeImpl
import com.aiassistant.framework.accessibility.ActionPerformer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FrameworkModule {

    @Binds
    @Singleton
    abstract fun bindServiceBridge(impl: AccessibilityServiceBridgeImpl): AccessibilityServiceBridge
}

@Module
@InstallIn(SingletonComponent::class)
object FrameworkProviderModule {

    @Provides
    @Singleton
    fun provideActionPerformer(bridge: AccessibilityServiceBridge): ActionPerformer {
        return ActionPerformer(bridge)
    }
}
```

**Estimated Time:** 10 minutes

**Total Phase 3 Time:** ~55 minutes

---

## Phase 4: Presentation Layer Updates (Week 3)

### 4.1 Update Chat Components

#### Step 4.1.1: Update ChatChatState.kt to use domain models

Replace app/src/main/java/com/aiassistant/presentation/chat/ChatState.kt:

```kotlin
package com.aiassistant.presentation.chat

import com.aiassistant.domain.model.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isExecuting: Boolean = false,
    val currentStep: String = "",
    val stepCount: Int = 0,
    val isServiceConnected: Boolean = false
)
```

**Estimated Time:** 2 minutes

#### Step 4.1.2: Update ChatChatIntent.kt with proper intents

Update app/src/main/java/com/aiassistant/presentation/chat/ChatIntent.kt:

```kotlin
package com.aiassistant.presentation.chat

sealed interface ChatIntent {
    data class ExecuteCommand(val command: String) : ChatIntent
    data object CancelExecution : ChatIntent
    data object ClearHistory : ChatIntent
    data class UpdateInput(val input: String) : ChatIntent
}
```

**Estimated Time:** 2 minutes

#### Step 4.1.3: Update ChatViewModel with full MVI implementation

Replace app/src/main/java/com/aiassistant/presentation/chat/ChatViewModel.kt:

```kotlin
package com.aiassistant.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.domain.model.*
import com.aiassistant.domain.usecase.ExecuteTaskUseCase
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.usecase.CancelTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val executeTaskUseCase: ExecuteTaskUseCase,
    private val screenRepository: ScreenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _sideEffect = Channel<ChatSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<ChatSideEffect> = _sideEffect.receiveAsFlow()

    private var executionJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                _state.update { it.copy(isServiceConnected = screenRepository.isServiceConnected()) }
                delay(2000)
            }
        }
    }

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> {
                _state.update { it.copy(inputText = intent.input) }
            }
            is ChatIntent.ExecuteCommand -> executeCommand(intent.command)
            is ChatIntent.CancelExecution -> cancelExecution()
            is ChatIntent.ClearHistory -> {
                _state.update { it.copy(messages = emptyList()) }
            }
        }
    }

    private fun executeCommand(command: String) {
        if (command.isBlank()) return
        if (!_state.value.isServiceConnected) {
            viewModelScope.launch {
                _sideEffect.send(ChatSideEffect.OpenAccessibilitySettings)
            }
            return
        }

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(content = command, isUser = true),
                inputText = "",
                isExecuting = true,
                currentStep = "Starting...",
                stepCount = 0
            )
        }
        _sideEffect.trySend(ChatSideEffect.ScrollToBottom)

        executionJob = viewModelScope.launch {
            try {
                executeTaskUseCase(command).collect { stepResult ->
                    _state.update {
                        it.copy(
                            currentStep = stepResult.thought,
                            stepCount = stepResult.stepNumber + 1
                        )
                    }

                    if (stepResult.status == TaskStatus.DONE ||
                        stepResult.status == TaskStatus.FAILED) {

                        val resultText = when (stepResult.status) {
                            TaskStatus.DONE -> "✅ Done! ${stepResult.thought}"
                            TaskStatus.FAILED -> "❌ Failed: ${stepResult.thought}"
                            else -> stepResult.thought
                        }
                        _state.update {
                            it.copy(
                                messages = it.messages + ChatMessage(
                                    content = resultText,
                                    isUser = false
                                ),
                                isExecuting = false
                            )
                        }
                        _sideEffect.send(ChatSideEffect.ScrollToBottom)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            content = "🛑 Task cancelled.",
                            isUser = false
                        ),
                        isExecuting = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isExecuting = false) }
                _sideEffect.send(ChatSideEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun cancelExecution() {
        executionJob?.cancel()
        _state.update { it.copy(isExecuting = false, currentStep = "Cancelled") }
    }
}
```

**Estimated Time:** 25 minutes

#### Step 4.1.4: Create ChatSideEffect.kt (new file)

```kotlin
package com.aiassistant.presentation.chat

sealed interface ChatSideEffect {
    data class ShowError(val message: String) : ChatSideEffect
    data object ScrollToBottom : ChatSideEffect
    data object OpenAccessibilitySettings : ChatSideEffect
}
```

**Estimated Time:** 2 minutes

**Total Phase 4.1 Time:** ~31 minutes

---

### 4.2 Update UI Components

#### Step 4.2.1: Create presentation/navigation/NavigationScreen.kt

```kotlin
package com.aiassistant.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface NavigationScreen {
    @Serializable
    data object Main
    @Serializable
    data object Settings
}
```

**Estimated Time:** 2 minutes

#### Step 4.2.2: Update presentation/navigation/NavigationStack.kt

Replace app/src/main/java/com/aiassistant/presentation/navigation/NavigationStack.kt:

```kotlin
package com.aiassistant.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiassistant.presentation.chat.ChatScreen

@Composable
fun NavigationStack() {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = NavigationScreen.Main) {
    composable<NavigationScreen.Main> {
      ChatScreen()
    }
  }
}
```

**Estimated Time:** 3 minutes

#### Step 4.2.3: Update ChatScreen.kt with full implementation

Update app/src/main/java/com/aiassistant/presentation/chat/ChatScreen.kt:

```kotlin
package com.aiassistant.presentation.chat

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ChatSideEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is ChatSideEffect.ScrollToBottom -> {
                    scope.launch {
                        if (state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(state.messages.lastIndex)
                        }
                    }
                }
                is ChatSideEffect.OpenAccessibilitySettings -> {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.isServiceConnected) {
                ServiceDisconnectedBanner(
                    onClick = { viewModel.processIntent(ChatIntent.ExecuteCommand("")) }
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
            }

            if (state.isExecuting) {
                StepIndicator(
                    currentStep = state.currentStep,
                    stepCount = state.stepCount,
                    onCancel = { viewModel.processIntent(ChatIntent.CancelExecution) }
                )
            }

            CommandInput(
                value = state.inputText,
                onValueChange = { viewModel.processIntent(ChatIntent.UpdateInput(it)) },
                onSend = { viewModel.processIntent(ChatIntent.ExecuteCommand(state.inputText)) },
                enabled = !state.isExecuting
            )
        }
    }
}
```

**Estimated Time:** 20 minutes

#### Step 4.2.4: Create component CommandInput.kt
```kotlin
package com.aiassistant.presentation.chat.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun CommandInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a command...") },
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() })
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Execute"
            )
        }
    }
}
```
**Estimated Time:** 10 minutes

#### Step 4.2.5: Create component StepIndicator.kt
```kotlin
package com.aiassistant.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StepIndicator(
    currentStep: String,
    stepCount: Int,
    onCancel: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Step $stepCount", style = MaterialTheme.typography.labelSmall)
                Text(currentStep, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            TextButton(onClick = onCancel) {
                Text("Stop")
            }
        }
    }
}
```
**Estimated Time:** 8 minutes

#### Step 4.2.6: Create component ChatBubble.kt
```kotlin
package com.aiassistant.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.domain.model.ChatMessage

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val alignment = if (isUser) androidx.compose.ui.Alignment.CenterEnd else androidx.compose.ui.Alignment.CenterStart

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Text(
            text = message.content,
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```
**Estimated Time:** 8 minutes

#### Step 4.2.7: Create component ServiceDisconnectedBanner.kt
```kotlin
package com.aiassistant.presentation.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ServiceDisconnectedBanner(onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Accessibility Service Not Connected",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Enable to use automation features",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = onClick) {
                Text("Settings")
            }
        }
    }
}
```
**Estimated Time:** 8 minutes

**Total Phase 4.2 Time:** ~68 minutes (1.1 hours)

---

### 4.3 Hilt App Configuration

#### Step 4.3.1: Update ClawdroidApp.kt with Hilt

```kotlin
package com.aiassistant.presentation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClawdroidApp : Application()
```

**Estimated Time:** 2 minutes

#### Step 4.3.2: Update MainActivity.kt with Hilt
```kotlin
package com.aiassistant.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aiassistant.presentation.navigation.NavigationStack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavigationStack()
        }
    }
}
```
**Estimated Time:** 3 minutes

**Total Phase 4 Time:** ~104 minutes (1.7 hours)

---

## Phase 5: Android Manifest & Resources (Week 3)

### 5.1 Manifest Updates

#### Step 5.1.1: Update AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:name=".presentation.ClawdroidApp"
        ...>

        <activity
            android:name=".presentation.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name="com.aiassistant.framework.accessibility.AutomatorAccessibilityService"
            android:exported="true"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>
</manifest>
```

**Estimated Time:** 15 minutes

#### Step 5.1.2: Create res/xml/accessibility_service_config.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagIncludeNotImportantViews|flagReportViewIds|flagRequestEnhancedWebAccessibility"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="true"
    android:notificationTimeout="100"
    android:description="@string/accessibility_description" />
```

**Estimated Time:** 5 minutes

#### Step 5.1.3: Add string resource for accessibility_description

In `res/values/strings.xml`:

```xml
<string name="accessibility_description">Automates UI actions to perform commands</string>
```

**Estimated Time:** 2 minutes

**Total Phase 5 Time:** ~22 minutes

---

## Phase 6: Dependencies Configuration (Week 3)

### 6.1 Update Gradle Version Catalog & Build Files

#### Step 6.1.1: Update gradle/libs.versions.toml with new dependencies
Add/update entries:
```toml
ktor = "2.3.7"
room = "2.6.1"

# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

# Room
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
```
**Estimated Time:** 10 minutes

#### Step 6.1.2: Update app/build.gradle.kts with all dependencies

Add all required dependencies to the existing app/build.gradle.kts:

- Hilt, Room, Ktor dependencies
- kotlinx.coroutines
- kotlinx.serialization
**Estimated Time:** 15 minutes

**Total Phase 6 Time:** ~25 minutes

---

## Phase 7: Testing (Week 4-5)

### 7.1 Unit Tests

#### Step 7.1.1: Create domain model unit tests

- [ ] Test UINode equality and bounds
- [ ] Test Action enum conversion
- [ ] Test ActionResponse parsing
  **Estimated Time:** 1 hour

#### Step 7.1.2: Create use case unit tests

- [ ] Test ExecuteTaskUseCase happy path
- [ ] Test ExecuteTaskUseCase max steps
- [ ] Test ExecuteTaskUseCase cancellation
- [ ] Test ExecuteTaskUseCase error handling
  **Estimated Time:** 2 hours

#### Step 7.1.3: Create mapper unit tests

- [ ] Test ScreenParser with mock nodes
- [ ] Test UINodeFormatter output format
- [ ] Test ActionResponseMapper JSON parsing
  **Estimated Time:** 2 hours

#### Step 7.1.4: Create repository unit tests

- [ ] Mock OpenAI API responses
- [ ] Test LlmRepositoryImpl prompt assembly
- [ ] Test ScreenRepositoryImpl delegation
  **Estimated Time:** 2 hours

**Total Phase 7 Time:** ~7 hours

---

### 7.2 Integration Tests

#### Step 7.2.1: Create Ktor client mock test

- [ ] Test with MockEngine returning sample GPT responses
  **Estimated Time:** 1 hour

#### Step 7.2.2: Create Room in-memory database test

- [ ] Test TaskLogDao insert and query
  **Estimated Time:** 1 hour

#### Step 7.2.3: Create end-to-end flow test

- [ ] Test complete command execution flow
  **Estimated Time:** 2 hours

**Total Phase 7.2 Time:** ~4 hours

---

## Phase 8: Settings & Persistence (Week 4-5)

### 8.1 API Key Management

#### Step 8.1.1: Create SettingsScreen composable

- [ ] Add OpenAI API key input field
- [ ] Securely store API key (EncryptedSharedPreferences)
  **Estimated Time:** 2 hours

#### Step 8.1.2: Create SettingsViewModel

- [ ] Handle API key save/load
- [ ] Connect to OpenAIApiService
  **Estimated Time:** 1 hour

#### Step 8.1.3: Create Navigation integration

- [ ] Add navigation between Chat and Settings screens
  **Estimated Time:** 1 hour

**Total Phase 8 Time:** ~4 hours

---

## Phase 9: E2E Manual Testing & Polish (Week 4-6)

### 9.1 Manual Testing Scenarios

Test each of these flows:

- [ ] "Play music on YouTube" - YouTube opens, searches, plays
- [ ] "Search Android developer jobs on LinkedIn" - LinkedIn opens, searches
- [ ] "Open Chrome and search for weather" - Chrome opens, types and searches
- [ ] "Send WhatsApp message to Mom" - WhatsApp opens, finds contact
  **Estimated Time:** 8 hours

### 9.2 Error Handling

- [ ] Add retry logic for failed actions
- [ ] Add timeout handling for LLM calls
- [ ] Add connection error handling
- [ ] Improve error messages
  **Estimated Time:** 4 hours

### 9.3 Polish

- [ ] Add loading states
- [ ] Animations for actions
- [ ] Better step indicators
- [ ] Progress feedback
  **Estimated Time:** 4 hours

**Total Phase 9 Time:** ~16 hours

---

## Phase 10: Documentation & Final Review

### 10.1 Documentation

- [ ] Add README with setup instructions
- [ ] Add API key setup guide
- [ ] Add accessibility setup guide
- [ ] Document architecture decisions
  **Estimated Time:** 3 hours

### 10.2 Code Review & Cleanup

- [ ] Remove unused imports
- [ ] Add missing Javadoc comments
- [ ] Ensure consistent code style
- [ ] Check for memory leaks
  **Estimated Time:** 4 hours

**Total Phase 10 Time:** ~7 hours

---

## 📈 Summary & Time Estimates

### By Phase

| Phase | Description | Time |
|-------|-------------|------|
| 1 | Multi-Module Setup & Domain Layer | ~3 hours |
| 2 | Data Layer Implementation | ~2 hours |
| 3 | Framework Layer | ~1 hour |
| 4 | Presentation Layer Updates | ~1.5 hours |
| 5 | Android Manifest & Resources | ~0.5 hours |
| 6 | Dependencies Configuration | ~0.5 hours |
| 7 | Testing | ~11 hours |
| 8 | Settings & Persistence | ~4 hours |
| 9 | E2E Testing & Polish | ~16 hours |
| 10 | Documentation & Final Review | ~7 hours |
| **Total** | | **~46.5 hours** |

### Remaining Work Breakdown

#### High Priority (Core Functionality)

- [ ] Create `:domain`, `:data`, `:framework` modules (3 hours)
- [ ] Implement domain models and use cases (2 hours)
- [ ] Implement data layer with LLM integration (4 hours)
- [ ] Implement framework layer with accessibility (2 hours)
- [ ] Update presentation layer with MVI (2 hours)

#### Medium Priority (Good Experience)

- [ ] Add Settings screen with API key management (4 hours)
- [ ] Implement error handling and retry logic (4 hours)
- [ ] Add progress indicators and visual feedback (4 hours)

#### Low Priority (Polish)

- [ ] Add animations and polish (4 hours)
- [ ] Write documentation (3 hours)
- [ ] Unit and integration tests (11 hours)

### Completion Checklist

#### Module Structure

- [ ] `:domain` module created with pure Kotlin
- [ ] `:data` module with repository implementations
- [ ] `:framework` module with Android-specific code
- [ ] `:app` updated to depend on other modules
- [ ] Gradle files configured correctly

#### Domain Layer

- [ ] All domain models created (UINode, Action, etc.)
- [ ] Repository interfaces defined
- [ ] Use cases implemented (ExecuteTask, CaptureScreen, etc.)

#### Data Layer

- [ ] ScreenParser converts AccessibilityNodeInfo to UINode
- [ ] UINodeFormatter formats screen state for LLM
- [ ] OpenAIApiService calls GPT-4o
- [ ] ActionResponseMapper parses JSON responses
- [ ] Repository implementations bridge to framework layer
- [ ] Hilt modules configured

#### Framework Layer

- [ ] AutomatorAccessibilityService reads UI trees
- [ ] AccessibilityServiceBridge abstracts service access
- [ ] ActionPerformer executes all action types
- [ ] Service properly registered in manifest

#### Presentation Layer

- [ ] ChatViewModel collects Flow of StepResults
- [ ] ChatScreen displays messages and progress
- [ ] UI components updated (ChatBubble, StepIndicator, etc.)
- [ ] Navigation between screens works
- [ ] Error handling with side effects

#### Configuration

- [ ] AndroidManifest.xml with accessibility service
- [ ] Accessibility service config XML
- [ ] All Gradle dependencies configured
- [ ] Hilt App class created
- [ ] Annotations added to classes

#### Testing

- [ ] Domain unit tests pass
- [ ] Data unit tests pass
- [ ] Integration tests pass
- [ ] E2E flows tested manually

