# Android AI Automation App — MVP Implementation Plan

**Architecture:** Clean Architecture · MVI · Coroutines/Flow · Hilt · Jetpack Compose
**Stack:** Kotlin · GPT-4o API · AccessibilityService · Room · Ktor
**Timeline:** 4–6 weeks

---

## 1. Architecture Overview

### 1.1 Layer Diagram

```
┌──────────────────────────────────────────────────────────────┐
│  PRESENTATION (app module)                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │  ChatScreen   │  │  SettingsScreen│ │  OverlayService   │ │
│  │  (Compose)    │  │  (Compose)    │ │  (Floating bubble) │ │
│  └──────┬───────┘  └──────┬───────┘ └────────┬───────────┘ │
│         │                  │                   │              │
│  ┌──────▼──────────────────▼───────────────────▼───────────┐ │
│  │                    MVI ViewModels                        │ │
│  │  ChatViewModel  SettingsViewModel  OverlayViewModel     │ │
│  │  ┌────────────────────────────────────────────────────┐ │ │
│  │  │  State: ChatUiState    Intent: ChatIntent          │ │ │
│  │  │  SideEffect: ChatSideEffect                        │ │ │
│  │  └────────────────────────────────────────────────────┘ │ │
│  └──────────────────────────┬──────────────────────────────┘ │
├─────────────────────────────┼────────────────────────────────┤
│  DOMAIN (domain module)     │  ← Pure Kotlin, zero Android   │
│  ┌──────────────────────────▼──────────────────────────────┐ │
│  │                      Use Cases                           │ │
│  │  ExecuteTaskUseCase     CaptureScreenUseCase            │ │
│  │  ParseCommandUseCase    CancelTaskUseCase               │ │
│  └──────────────────────────┬──────────────────────────────┘ │
│  ┌──────────────────────────▼──────────────────────────────┐ │
│  │                   Repository Interfaces                  │ │
│  │  ScreenRepository   LlmRepository   TaskLogRepository   │ │
│  └──────────────────────────┬──────────────────────────────┘ │
│  ┌──────────────────────────▼──────────────────────────────┐ │
│  │                    Domain Models                         │ │
│  │  UINode  Action  ActionResponse  TaskResult  Bounds     │ │
│  └─────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────┤
│  DATA (data module)          ← Implements domain interfaces  │
│  ┌─────────────────────┐ ┌─────────────────────────────────┐│
│  │  Repository Impls    │ │  Mappers                        ││
│  │  ScreenRepoImpl      │ │  ScreenParser                   ││
│  │  LlmRepoImpl         │ │  ActionResponseMapper           ││
│  │  TaskLogRepoImpl     │ │  NodeToUINodeMapper              ││
│  └─────────────────────┘ └─────────────────────────────────┘│
│  ┌─────────────────────┐ ┌─────────────────────────────────┐│
│  │  Remote (API)        │ │  Local (Room)                   ││
│  │  OpenAIApiService    │ │  TaskLogDao                     ││
│  │  DTOs / RequestBody  │ │  TaskLogEntity                  ││
│  └─────────────────────┘ └─────────────────────────────────┘│
├──────────────────────────────────────────────────────────────┤
│  FRAMEWORK (framework module) ← Android-specific services    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │  AutomatorAccessibilityService                          ││
│  │  TaskForegroundService                                  ││
│  │  ActionPerformer (clicks, scrolls, types on device)     ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
```

### 1.2 MVI Flow

```
User types command
       │
       ▼
  ChatIntent.ExecuteCommand("Play music on YouTube")
       │
       ▼
  ChatViewModel.processIntent()
       │
       ├─► emit ChatUiState(isLoading = true, currentStep = "Launching YouTube...")
       │
       ▼
  ExecuteTaskUseCase.invoke()
       │
       ├─► ScreenRepository.captureScreen()        → List<UINode>
       ├─► LlmRepository.getNextActions(screen, command) → ActionResponse
       ├─► ScreenRepository.performAction(action)   → Boolean
       ├─► emit intermediate StepResult via Flow
       └─► loop until status == "done" or "failed"
       │
       ▼
  ChatUiState(isLoading = false, messages = [..., result])
```

### 1.3 Module Dependency Rule

```
:app (presentation) ──depends on──► :domain
:data             ──depends on──► :domain
:framework        ──depends on──► :domain
:app              ──depends on──► :data, :framework  (only via Hilt wiring)

:domain depends on NOTHING (pure Kotlin module)
```

---

## 2. Full Package Structure

```
project-root/
├── app/                                    # :app module (presentation)
│   └── src/main/java/com/app/automator/
│       ├── App.kt                          # @HiltAndroidApp
│       ├── MainActivity.kt                 # @AndroidEntryPoint, NavHost
│       ├── navigation/
│       │   └── AppNavGraph.kt
│       ├── ui/
│       │   ├── chat/
│       │   │   ├── ChatScreen.kt           # @Composable
│       │   │   ├── ChatViewModel.kt        # @HiltViewModel, MVI
│       │   │   ├── ChatUiState.kt          # data class (state)
│       │   │   ├── ChatIntent.kt           # sealed interface (user intents)
│       │   │   ├── ChatSideEffect.kt       # sealed interface (one-shots)
│       │   │   └── components/
│       │   │       ├── ChatBubble.kt
│       │   │       ├── StepIndicator.kt
│       │   │       └── CommandInput.kt
│       │   ├── settings/
│       │   │   ├── SettingsScreen.kt
│       │   │   ├── SettingsViewModel.kt
│       │   │   ├── SettingsUiState.kt
│       │   │   └── SettingsIntent.kt
│       │   ├── onboarding/
│       │   │   └── AccessibilityOnboardingScreen.kt
│       │   └── theme/
│       │       ├── Theme.kt
│       │       ├── Color.kt
│       │       └── Type.kt
│       └── service/
│           └── OverlayService.kt           # floating bubble (post-MVP)
│
├── domain/                                 # :domain module (pure Kotlin)
│   └── src/main/java/com/app/automator/domain/
│       ├── model/
│       │   ├── UINode.kt                   # screen element representation
│       │   ├── Bounds.kt                   # replaces android.graphics.Rect
│       │   ├── Action.kt                   # LLM-returned action
│       │   ├── ActionResponse.kt           # full LLM response wrapper
│       │   ├── TaskResult.kt               # Success / Failed / MaxSteps / Cancelled
│       │   ├── StepResult.kt               # intermediate step outcome
│       │   ├── ChatMessage.kt              # UI message model
│       │   └── AppTarget.kt               # known app package mappings
│       ├── repository/
│       │   ├── ScreenRepository.kt         # interface
│       │   ├── LlmRepository.kt            # interface
│       │   └── TaskLogRepository.kt        # interface
│       └── usecase/
│           ├── ExecuteTaskUseCase.kt        # main orchestration loop
│           ├── CaptureScreenUseCase.kt      # single screen capture
│           ├── CancelTaskUseCase.kt         # cancellation signal
│           └── GetTaskHistoryUseCase.kt     # retrieve past task logs
│
├── data/                                   # :data module
│   └── src/main/java/com/app/automator/data/
│       ├── repository/
│       │   ├── ScreenRepositoryImpl.kt      # delegates to framework
│       │   ├── LlmRepositoryImpl.kt         # calls OpenAI API
│       │   └── TaskLogRepositoryImpl.kt     # Room persistence
│       ├── remote/
│       │   ├── OpenAIApiService.kt          # Ktor HTTP client
│       │   ├── dto/
│       │   │   ├── ChatCompletionRequest.kt
│       │   │   ├── ChatCompletionResponse.kt
│       │   │   └── MessageDto.kt
│       │   └── interceptor/
│       │       └── AuthInterceptor.kt       # injects API key
│       ├── local/
│       │   ├── AppDatabase.kt               # Room database
│       │   ├── dao/
│       │   │   └── TaskLogDao.kt
│       │   └── entity/
│       │       └── TaskLogEntity.kt
│       ├── mapper/
│       │   ├── ScreenParser.kt              # AccessibilityNodeInfo → UINode
│       │   ├── ActionResponseMapper.kt      # API JSON → domain ActionResponse
│       │   ├── UINodeFormatter.kt           # UINode list → LLM-readable string
│       │   └── TaskLogMapper.kt             # Entity ↔ Domain
│       └── di/
│           ├── DataModule.kt                # @Module: repos, DB, API client
│           └── NetworkModule.kt             # @Module: Ktor client config
│
├── framework/                              # :framework module
│   └── src/main/java/com/app/automator/framework/
│       ├── accessibility/
│       │   ├── AutomatorAccessibilityService.kt  # the Android service
│       │   ├── AccessibilityServiceBridge.kt     # interface for data layer
│       │   └── ActionPerformer.kt                # click, scroll, setText, launch
│       ├── service/
│       │   └── TaskForegroundService.kt          # keeps process alive
│       └── di/
│           └── FrameworkModule.kt                # @Module: service bindings
│
└── build files
    ├── settings.gradle.kts         # include(":app", ":domain", ":data", ":framework")
    └── build.gradle.kts (per module)
```

---

## 3. Domain Layer (Pure Kotlin)

The domain module has **zero Android dependencies**. It defines the contracts and business logic. You can unit-test everything here with plain JUnit — no Robolectric, no instrumented tests.

### 3.1 Domain Models

```kotlin
// domain/model/Bounds.kt
// Pure replacement for android.graphics.Rect — keeps domain Android-free
data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
```

```kotlin
// domain/model/UINode.kt
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

```kotlin
// domain/model/Action.kt
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

```kotlin
// domain/model/ActionResponse.kt
data class ActionResponse(
    val thought: String,
    val status: TaskStatus,
    val actions: List<Action>
)

enum class TaskStatus { ACTING, DONE, FAILED }
```

```kotlin
// domain/model/TaskResult.kt
sealed interface TaskResult {
    data class Success(val summary: String, val stepsUsed: Int) : TaskResult
    data class Failed(val reason: String, val stepsUsed: Int) : TaskResult
    data object MaxStepsReached : TaskResult
    data object Cancelled : TaskResult
}
```

```kotlin
// domain/model/StepResult.kt
// Emitted per iteration so the UI can show live progress
data class StepResult(
    val stepNumber: Int,
    val thought: String,
    val actionsTaken: List<Action>,
    val status: TaskStatus
)
```

```kotlin
// domain/model/ChatMessage.kt
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
```

```kotlin
// domain/model/AppTarget.kt
// Known app mappings — used in system prompt and for launch actions
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

### 3.2 Repository Interfaces

```kotlin
// domain/repository/ScreenRepository.kt
interface ScreenRepository {
    suspend fun captureScreen(): List<UINode>
    suspend fun performAction(action: Action): Boolean
    fun isServiceConnected(): Boolean
}
```

```kotlin
// domain/repository/LlmRepository.kt
interface LlmRepository {
    suspend fun getNextActions(
        screenState: List<UINode>,
        userCommand: String,
        conversationHistory: List<Pair<String, String>> // role → content
    ): ActionResponse
}
```

```kotlin
// domain/repository/TaskLogRepository.kt
interface TaskLogRepository {
    suspend fun saveLog(command: String, result: TaskResult, steps: List<StepResult>)
    fun getRecentLogs(limit: Int): Flow<List<TaskLog>>
}
```

### 3.3 Use Cases

```kotlin
// domain/usecase/ExecuteTaskUseCase.kt
// The core orchestration loop. Returns a Flow so the ViewModel gets live updates.
class ExecuteTaskUseCase @Inject constructor(
    private val screenRepo: ScreenRepository,
    private val llmRepo: LlmRepository,
    private val taskLogRepo: TaskLogRepository
) {
    companion object {
        private const val MAX_STEPS = 20
        private const val SETTLE_DELAY_MS = 1500L
    }

    operator fun invoke(command: String): Flow<StepResult> = flow {
        val history = mutableListOf<Pair<String, String>>()
        var stepCount = 0

        while (stepCount < MAX_STEPS) {
            // Check cancellation (cooperative via coroutine)
            currentCoroutineContext().ensureActive()

            // 1. Capture screen
            val screen = screenRepo.captureScreen()
            if (screen.isEmpty()) {
                emit(StepResult(stepCount, "Cannot read screen", emptyList(), TaskStatus.FAILED))
                return@flow
            }

            // 2. Ask LLM
            val response = llmRepo.getNextActions(screen, command, history)

            // 3. Emit progress
            emit(StepResult(stepCount, response.thought, response.actions, response.status))

            // 4. Check terminal states
            if (response.status == TaskStatus.DONE || response.status == TaskStatus.FAILED) {
                return@flow
            }

            // 5. Execute each action
            for (action in response.actions) {
                val success = screenRepo.performAction(action)
                if (!success) {
                    history.add("user" to "Action ${action.type} on index ${action.index} FAILED")
                }
            }

            // 6. Wait for UI to settle
            delay(SETTLE_DELAY_MS)

            // 7. Update conversation history
            history.add("assistant" to response.thought)
            stepCount++
        }

        emit(StepResult(stepCount, "Maximum steps reached", emptyList(), TaskStatus.FAILED))
    }.flowOn(Dispatchers.Default)
}
```

```kotlin
// domain/usecase/CancelTaskUseCase.kt
// Cancellation is handled by cancelling the coroutine Job in the ViewModel.
// This use case exists if you need additional cleanup logic.
class CancelTaskUseCase @Inject constructor() {
    // The ViewModel cancels the collection Job directly.
    // This use case is a placeholder for future cleanup (e.g., undo last action).
    operator fun invoke() { /* no-op for MVP */ }
}
```

---

## 4. Data Layer

### 4.1 Screen Parser (Mapper)

```kotlin
// data/mapper/ScreenParser.kt
// Converts AccessibilityNodeInfo tree → domain UINode list.
// This is the ONLY class that touches AccessibilityNodeInfo.
class ScreenParser @Inject constructor() {

    private var nodeIndex = 0
    // Keeps a reference map so ActionPerformer can find real nodes by index
    private val _nodeMap = mutableMapOf<Int, AccessibilityNodeInfo>()
    val nodeMap: Map<Int, AccessibilityNodeInfo> get() = _nodeMap

    fun parse(rootNode: AccessibilityNodeInfo): List<UINode> {
        nodeIndex = 0
        _nodeMap.clear()
        return walkTree(rootNode)
    }

    private fun walkTree(node: AccessibilityNodeInfo): List<UINode> {
        if (!node.isVisibleToUser) return emptyList()

        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)

        val children = mutableListOf<UINode>()
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { children.addAll(walkTree(it)) }
        }

        val isRelevant = node.isClickable || node.isScrollable || node.isEditable
            || !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()

        return if (isRelevant) {
            val idx = nodeIndex++
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

```kotlin
// data/mapper/UINodeFormatter.kt
// Converts UINode list → flattened text for the LLM prompt
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

### 4.2 LLM Repository Implementation

```kotlin
// data/repository/LlmRepositoryImpl.kt
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

```kotlin
// data/remote/OpenAIApiService.kt
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

### 4.3 Screen Repository Implementation

```kotlin
// data/repository/ScreenRepositoryImpl.kt
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

### 4.4 System Prompt

```kotlin
// data/repository/SystemPrompt.kt
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

### 4.5 DTOs

```kotlin
// data/remote/dto/ChatCompletionRequest.kt
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

```kotlin
// data/remote/dto/ChatCompletionResponse.kt
@Serializable
data class ChatCompletionResponse(
    val choices: List<ChoiceDto>
)

@Serializable
data class ChoiceDto(
    val message: MessageDto
)
```

### 4.6 Action Response Mapper

```kotlin
// data/mapper/ActionResponseMapper.kt
class ActionResponseMapper @Inject constructor() {

    fun toDomain(response: ChatCompletionResponse): ActionResponse {
        val json = response.choices.first().message.content
        val raw = Json.decodeFromString<RawActionResponse>(json)

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
            direction = raw.direction?.let {
                ScrollDirection.valueOf(it.uppercase())
            }
        )
    }
}

// Internal raw models for JSON deserialization
@Serializable
private data class RawActionResponse(
    val thought: String,
    val status: String,
    val actions: List<RawAction>
)

@Serializable
private data class RawAction(
    val type: String,
    val index: Int? = null,
    @SerialName("package") val packageName: String? = null,
    val text: String? = null,
    val direction: String? = null
)
```

### 4.7 Room (Task Log)

```kotlin
// data/local/entity/TaskLogEntity.kt
@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val resultType: String,     // "success", "failed", "max_steps", "cancelled"
    val resultSummary: String,
    val stepsUsed: Int,
    val timestamp: Long
)
```

```kotlin
// data/local/dao/TaskLogDao.kt
@Dao
interface TaskLogDao {
    @Insert
    suspend fun insert(log: TaskLogEntity)

    @Query("SELECT * FROM task_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TaskLogEntity>>
}
```

---

## 5. Framework Layer

### 5.1 Accessibility Service

```kotlin
// framework/accessibility/AutomatorAccessibilityService.kt
class AutomatorAccessibilityService : AccessibilityService() {

    companion object {
        // Observable connection state
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* no-op for MVP */ }
    override fun onInterrupt() { /* no-op */ }
}
```

### 5.2 Service Bridge (Interface for Data Layer)

```kotlin
// framework/accessibility/AccessibilityServiceBridge.kt
// Abstracts the static companion access into an injectable interface
interface AccessibilityServiceBridge {
    fun getRootNode(): AccessibilityNodeInfo?
    fun isConnected(): Boolean
    fun performGlobalAction(action: Int): Boolean
    fun getContext(): Context?
}

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

### 5.3 Action Performer

```kotlin
// framework/accessibility/ActionPerformer.kt
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
        // Focus the node first
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
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (intent != null) {
            context.startActivity(intent)
            true
        } else false
    }
}
```

---

## 6. Presentation Layer (MVI)

### 6.1 MVI Contracts

```kotlin
// ui/chat/ChatIntent.kt
sealed interface ChatIntent {
    data class ExecuteCommand(val command: String) : ChatIntent
    data object CancelExecution : ChatIntent
    data object ClearHistory : ChatIntent
    data class UpdateInput(val text: String) : ChatIntent
}
```

```kotlin
// ui/chat/ChatUiState.kt
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isExecuting: Boolean = false,
    val currentStep: String = "",
    val stepCount: Int = 0,
    val isServiceConnected: Boolean = false
)
```

```kotlin
// ui/chat/ChatSideEffect.kt
sealed interface ChatSideEffect {
    data class ShowError(val message: String) : ChatSideEffect
    data object ScrollToBottom : ChatSideEffect
    data object OpenAccessibilitySettings : ChatSideEffect
}
```

### 6.2 ViewModel

```kotlin
// ui/chat/ChatViewModel.kt
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val executeTaskUseCase: ExecuteTaskUseCase,
    private val screenRepository: ScreenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _sideEffect = Channel<ChatSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<ChatSideEffect> = _sideEffect.receiveAsFlow()

    private var executionJob: Job? = null

    init {
        // Monitor accessibility service connection
        viewModelScope.launch {
            // Poll periodically (or observe the StateFlow from the service)
            while (true) {
                _state.update { it.copy(isServiceConnected = screenRepository.isServiceConnected()) }
                delay(2000)
            }
        }
    }

    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> {
                _state.update { it.copy(inputText = intent.text) }
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

        // Add user message
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

        // Launch execution and collect step results
        executionJob = viewModelScope.launch {
            try {
                executeTaskUseCase(command).collect { stepResult ->
                    _state.update {
                        it.copy(
                            currentStep = stepResult.thought,
                            stepCount = stepResult.stepNumber + 1
                        )
                    }

                    // On terminal state, add result message
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
            } catch (e: CancellationException) {
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

### 6.3 Chat Screen

```kotlin
// ui/chat/ChatScreen.kt
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Collect side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ChatSideEffect.ShowError -> {
                    // Show snackbar or toast
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
            // Service connection banner
            if (!state.isServiceConnected) {
                ServiceDisconnectedBanner(
                    onClick = {
                        viewModel.processIntent(ChatIntent.ExecuteCommand(""))
                        // triggers side effect to open settings
                    }
                )
            }

            // Messages list
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

            // Execution progress
            if (state.isExecuting) {
                StepIndicator(
                    currentStep = state.currentStep,
                    stepCount = state.stepCount,
                    onCancel = { viewModel.processIntent(ChatIntent.CancelExecution) }
                )
            }

            // Input bar
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

```kotlin
// ui/chat/components/CommandInput.kt
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
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

```kotlin
// ui/chat/components/StepIndicator.kt
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

---

## 7. Dependency Injection (Hilt)

### 7.1 Data Module

```kotlin
// data/di/DataModule.kt
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

### 7.2 Network Module

```kotlin
// data/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(apiKeyProvider: ApiKeyProvider): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                header("Authorization", "Bearer ${apiKeyProvider.getKey()}")
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

### 7.3 Framework Module

```kotlin
// framework/di/FrameworkModule.kt
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

### 7.4 Database Module

```kotlin
// data/di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "automator_db").build()
    }

    @Provides
    fun provideTaskLogDao(db: AppDatabase): TaskLogDao = db.taskLogDao()
}
```

---

## 8. Manifest & Configuration

```xml
<!-- AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:name=".App"
        ...>

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Accessibility Service -->
        <service
            android:name="com.app.automator.framework.accessibility.AutomatorAccessibilityService"
            android:exported="true"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

        <!-- Foreground Service (for long tasks) -->
        <service
            android:name="com.app.automator.framework.service.TaskForegroundService"
            android:foregroundServiceType="specialUse"
            android:exported="false" />

    </application>
</manifest>
```

```xml
<!-- res/xml/accessibility_service_config.xml -->
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

---


---

## 10. End-to-End Flow: "Play music on YouTube"

| Step | Layer | Class | What Happens |
|------|-------|-------|--------------|
| 1 | Presentation | `ChatScreen` | User types "Play music on YouTube", taps Send |
| 2 | Presentation | `ChatViewModel` | Receives `ChatIntent.ExecuteCommand`, emits loading state |
| 3 | Domain | `ExecuteTaskUseCase` | Starts Flow, calls `ScreenRepository.captureScreen()` |
| 4 | Data | `ScreenRepositoryImpl` | Delegates to `AccessibilityServiceBridge.getRootNode()` |
| 5 | Framework | `AccessibilityServiceBridgeImpl` | Returns `rootInActiveWindow` from the live service |
| 6 | Data | `ScreenParser` | Walks `AccessibilityNodeInfo` tree → `List<UINode>` |
| 7 | Data | `UINodeFormatter` | Converts `UINode` list → flattened text |
| 8 | Data | `LlmRepositoryImpl` | Builds prompt, sends to GPT-4o via `OpenAIApiService` |
| 9 | External | GPT-4o | Returns `{"type":"launch","package":"com.google.android.youtube"}` |
| 10 | Data | `ActionResponseMapper` | Parses JSON → domain `ActionResponse` |
| 11 | Domain | `ExecuteTaskUseCase` | Emits `StepResult`, calls `ScreenRepository.performAction()` |
| 12 | Data | `ScreenRepositoryImpl` | Delegates to `ActionPerformer.execute()` |
| 13 | Framework | `ActionPerformer` | Launches YouTube via `packageManager` Intent |
| 14 | Domain | `ExecuteTaskUseCase` | Waits 1.5s, captures new screen, loops back to step 8 |
| 15 | — | — | Repeats: click search → type "music" → click result |
| 16 | Domain | `ExecuteTaskUseCase` | LLM returns `status: "done"`, emits final `StepResult` |
| 17 | Presentation | `ChatViewModel` | Updates state with success message, stops loading |
| 18 | Presentation | `ChatScreen` | Recomposes with new message: "✅ Done! Music is playing" |

---

## 11. Testing Strategy

### Unit Tests (domain module — plain JUnit)

| Class | What to Test |
|-------|-------------|
| `ExecuteTaskUseCase` | Happy path flow emission, max steps, cancellation, error propagation |
| `Action` / `ActionResponse` | Enum mapping, default values |
| `UINode` | Bounds equality, children nesting |

### Unit Tests (data module — MockK)

| Class | What to Test |
|-------|-------------|
| `ActionResponseMapper` | Valid JSON → domain model, malformed JSON → exception |
| `UINodeFormatter` | Flattened output format matches expected LLM input |
| `ScreenParser` | Mock `AccessibilityNodeInfo` → correct `UINode` tree |
| `LlmRepositoryImpl` | Correct prompt assembly, API error handling |

### Integration Tests

| Scenario | Approach |
|----------|----------|
| OpenAI API round-trip | Real API call with test key, validate JSON schema |
| Room persistence | In-memory DB, insert + query task logs |
| Ktor client | `MockEngine` returning sample GPT responses |

### Manual E2E Tests

| Command | Expected Outcome |
|---------|-----------------|
| "Play some music on YouTube" | YouTube opens, searches, plays first result |
| "Search Android developer jobs on LinkedIn" | LinkedIn Jobs tab, search executes |
| "Open Chrome and search for weather" | Chrome opens, types in search bar |
| "Send a WhatsApp message to Mom saying hello" | WhatsApp opens, finds contact, sends message |

---

## 12. Implementation Timeline

| Week | Phase | Key Deliverables |
|------|-------|-----------------|
| 1 | Project setup + Accessibility | Multi-module Gradle project, AccessibilityService reads UI trees, ScreenParser produces UINode output |
| 2 | LLM integration | OpenAI client, system prompt, ActionResponseMapper, verified JSON round-trips |
| 2–3 | Executor + orchestrator | ActionPerformer executes all 6 action types, ExecuteTaskUseCase loop works end-to-end |
| 3–4 | MVI + Compose UI | ChatScreen, ChatViewModel with MVI, StepIndicator, side effects, onboarding screen |
| 4–6 | Testing + polish | 5+ reliable E2E flows, error handling, retry logic, Room logging, API key settings screen |