# Koog AI Framework Migration Plan

## Overview

Migrate Clawdroid from custom LLM orchestration to [Koog AI](https://docs.koog.ai/basic-agents/) framework. This replaces the manual "capture screen → send to LLM → parse JSON → execute action → loop" with Koog's tool-based agent that handles orchestration automatically.

## Architecture Comparison

```
BEFORE (Without Koog):
  ChatViewModel → ExecuteTaskUseCase → ScreenRepository → LlmRepository
  → manual orchestration loop → ActionResponseMapper → ActionPerformer

AFTER (With Koog):
  ChatViewModel → agent.run(command)
  Koog internally: LLM → tool calls → DeviceTools → feedback → loop
```

---

## 1. Gradle Dependencies

### `gradle/libs.versions.toml`
```toml
[versions]
koog = "0.6.0"
kotlinx-serialization = "1.8.1"  # Update from 1.8.0

[libraries]
koog-agents = { group = "ai.koog", name = "koog-agents", version.ref = "koog" }
```

### `app/build.gradle.kts`
```kotlin
dependencies {
    implementation(libs.koog.agents)
}
```

---

## 2. File Structure Changes

### Files to DELETE
```
domain/usecase/ExecuteTaskUseCase.kt
domain/repository/LlmRepository.kt
domain/model/ActionResponse.kt
domain/model/StepResult.kt
data/repository/LlmRepositoryImpl.kt
data/repository/SystemPrompt.kt
data/mapper/ActionResponseMapper.kt
data/remote/OpenAIApiService.kt
data/remote/dto/ChatCompletionRequest.kt
data/remote/dto/ChatCompletionResponse.kt
```

### Files to CREATE
```
agent/DeviceTools.kt           # ToolSet wrapping accessibility
agent/AgentConfig.kt           # Configuration data class
agent/AndroidAgentFactory.kt   # Factory for agent creation
agent/AgentEventProcessor.kt   # Progress events for UI
agent/SystemPrompts.kt         # System prompt constants
agent/di/AgentModule.kt        # Hilt DI module
```

### Files to MODIFY
```
presentation/chat/ChatViewModel.kt  # Use agent.run() instead of use case
presentation/chat/ChatState.kt      # Add currentTool field
data/di/DataModule.kt               # Remove LlmRepository binding
data/di/NetworkModule.kt            # Remove OpenAI-specific code
```

### Files to KEEP (Android Framework Layer)
```
framework/accessibility/AutomatorAccessibilityService.kt
framework/accessibility/AccessibilityServiceBridge.kt
framework/accessibility/ActionPerformer.kt
data/repository/ScreenRepositoryImpl.kt
data/mapper/ScreenParser.kt
data/mapper/UINodeFormatter.kt
domain/repository/ScreenRepository.kt
domain/model/UINode.kt
domain/model/Action.kt
domain/model/Bounds.kt
```

---

## 3. DeviceTools Implementation

```kotlin
package com.aiassistant.agent

@LLMDescription("Android device automation tools")
class DeviceTools @Inject constructor(
    private val screenRepository: ScreenRepository,
    private val uiNodeFormatter: UINodeFormatter
) : ToolSet {

    @Tool
    @LLMDescription("Get current screen UI tree with element indices")
    suspend fun getScreen(): String {
        val nodes = screenRepository.captureScreen()
        return if (nodes.isEmpty()) "ERROR: Cannot read screen"
        else uiNodeFormatter.format(nodes)
    }

    @Tool
    @LLMDescription("Click element by index")
    suspend fun click(@LLMDescription("Element index") index: Int): String {
        val action = Action(type = ActionType.CLICK, index = index)
        val success = screenRepository.performAction(action)
        return if (success) "Clicked [$index]" else "FAILED: Click [$index]"
    }

    @Tool
    @LLMDescription("Type text into editable field")
    suspend fun setText(
        @LLMDescription("Field index") index: Int,
        @LLMDescription("Text to type") text: String
    ): String {
        val action = Action(type = ActionType.SET_TEXT, index = index, text = text)
        val success = screenRepository.performAction(action)
        return if (success) "Typed '$text' into [$index]" else "FAILED"
    }

    @Tool
    @LLMDescription("Scroll element in direction")
    suspend fun scroll(
        @LLMDescription("Element index") index: Int,
        @LLMDescription("up/down/left/right") direction: String
    ): String {
        val scrollDir = ScrollDirection.valueOf(direction.uppercase())
        val action = Action(type = ActionType.SCROLL, index = index, direction = scrollDir)
        return if (screenRepository.performAction(action)) "Scrolled $direction" else "FAILED"
    }

    @Tool
    @LLMDescription("Launch app by name")
    suspend fun launchApp(@LLMDescription("App name") appName: String): String {
        val action = Action(type = ActionType.LAUNCH, packageName = appName)
        return if (screenRepository.performAction(action)) "Launched $appName" else "FAILED"
    }

    @Tool
    @LLMDescription("Press back button")
    suspend fun pressBack(): String {
        val action = Action(type = ActionType.BACK)
        return if (screenRepository.performAction(action)) "Pressed back" else "FAILED"
    }

    @Tool
    @LLMDescription("Press home button")
    suspend fun pressHome(): String {
        val action = Action(type = ActionType.HOME)
        return if (screenRepository.performAction(action)) "Pressed home" else "FAILED"
    }

    @Tool
    @LLMDescription("Wait for screen to settle after action")
    suspend fun waitForUpdate(@LLMDescription("Milliseconds") ms: Int = 1500): String {
        delay(ms.coerceIn(100, 5000).toLong())
        return "Waited ${ms}ms"
    }

    @Tool
    @LLMDescription("Signal task completion")
    fun taskComplete(@LLMDescription("Summary") summary: String): String {
        return "TASK_COMPLETE: $summary"
    }

    @Tool
    @LLMDescription("Signal task failure")
    fun taskFailed(@LLMDescription("Reason") reason: String): String {
        return "TASK_FAILED: $reason"
    }
}
```

---

## 4. Agent Factory

```kotlin
package com.aiassistant.agent

@Singleton
class AndroidAgentFactory @Inject constructor(
    private val deviceTools: DeviceTools
) {
    fun createAgent(config: AgentConfig): AIAgent {
        val executor = when (config.provider) {
            LLMProvider.OPENAI -> simpleOpenAIExecutor(config.apiKey)
            LLMProvider.ANTHROPIC -> simpleAnthropicExecutor(config.apiKey)
            LLMProvider.GOOGLE -> simpleGoogleExecutor(config.apiKey)
        }

        return AIAgent(
            promptExecutor = executor,
            systemPrompt = SystemPrompts.ANDROID_AUTOMATION,
            llmModel = resolveModel(config),
            temperature = config.temperature,
            maxIterations = config.maxIterations,
            toolRegistry = ToolRegistry {
                tools(deviceTools)
            }
        )
    }
}

data class AgentConfig(
    val provider: LLMProvider = LLMProvider.OPENAI,
    val model: String = "gpt-4o",
    val apiKey: String,
    val temperature: Double = 0.1,
    val maxIterations: Int = 50
)

enum class LLMProvider { OPENAI, ANTHROPIC, GOOGLE }
```

---

## 5. System Prompt

```kotlin
object SystemPrompts {
    const val ANDROID_AUTOMATION = """
You are an Android automation agent. Control the device through tools.

WORKFLOW:
1. Call getScreen() to see current UI state
2. Analyze elements and their [index] numbers
3. Perform ONE action at a time
4. Call waitForUpdate() after screen-changing actions
5. Call getScreen() to verify result
6. Repeat until done

GUIDELINES:
- Elements marked "clickable" can be clicked
- Elements marked "scrollable" can be scrolled
- Elements marked "editable" accept text input
- If target not visible, try scrolling
- Launch apps with launchApp()

COMPLETION:
- Call taskComplete(summary) when finished
- Call taskFailed(reason) if impossible
"""
}
```

---

## 6. Event Processor (for UI updates)

```kotlin
class AgentEventProcessor : FeatureMessageProcessor() {
    private val _progress = MutableSharedFlow<AgentProgress>()
    val progress: SharedFlow<AgentProgress> = _progress.asSharedFlow()

    override suspend fun processMessage(message: FeatureMessage) {
        when (message) {
            is AgentStartingEvent -> _progress.emit(AgentProgress.Started)
            is ToolCallStartingEvent -> _progress.emit(
                AgentProgress.ToolExecuting(message.tool.name)
            )
            is AgentCompletedEvent -> _progress.emit(
                AgentProgress.Completed(message.result?.toString())
            )
            is AgentExecutionFailedEvent -> _progress.emit(
                AgentProgress.Failed(message.error.message)
            )
        }
    }
}

sealed interface AgentProgress {
    data object Started : AgentProgress
    data class ToolExecuting(val toolName: String) : AgentProgress
    data class Completed(val result: String?) : AgentProgress
    data class Failed(val error: String?) : AgentProgress
}
```

---

## 7. ViewModel Integration

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentFactory: AndroidAgentFactory,
    private val apiKeyProvider: ApiKeyProvider,
    private val screenRepository: ScreenRepository
) : ViewModel() {

    private fun executeCommand(command: String) {
        executionJob = viewModelScope.launch {
            val config = AgentConfig(
                provider = LLMProvider.OPENAI,
                apiKey = apiKeyProvider.getApiKey()
            )

            val processor = AgentEventProcessor()

            // Collect progress for UI
            launch {
                processor.progress.collect { handleProgress(it) }
            }

            val agent = agentFactory.createAgent(config).apply {
                install(Tracing) { addMessageProcessor(processor) }
            }

            val result = agent.run(command)  // That's it!

            // Handle result...
        }
    }
}
```

---

## 8. Benefits of Migration

| Aspect | Before (Custom) | After (Koog) |
|--------|-----------------|--------------|
| Orchestration loop | Manual in ExecuteTaskUseCase | Koog handles automatically |
| Tool feedback | LLM never sees if action succeeded | Immediate tool result feedback |
| Multi-action | One action per LLM call | Multiple tools per turn |
| Schema handling | Nullable-enum JSON headaches | Annotation-based, automatic |
| History compression | Manual | Built-in |
| Retry logic | Manual | Built-in |
| Provider switching | Rewrite API layer | Change executor |
| Lines of code | ~500+ in orchestration layer | ~200 in agent layer |

---

## 9. Implementation Phases

### Phase 1: Setup (1 day)
- [ ] Add Koog dependencies to gradle
- [ ] Update kotlinx-serialization version
- [ ] Verify build succeeds

### Phase 2: Create Agent Layer (2 days)
- [ ] Create `agent/` package
- [ ] Implement DeviceTools.kt
- [ ] Implement SystemPrompts.kt
- [ ] Implement AgentConfig.kt
- [ ] Implement AndroidAgentFactory.kt
- [ ] Implement AgentEventProcessor.kt
- [ ] Create di/AgentModule.kt

### Phase 3: Integrate ViewModel (1 day)
- [ ] Update ChatState.kt
- [ ] Rewrite ChatViewModel.kt
- [ ] Update DI modules
- [ ] Test basic flow

### Phase 4: Cleanup (1 day)
- [ ] Delete orchestration layer files
- [ ] Delete OpenAI API files
- [ ] Delete unused models
- [ ] Clean up imports

### Phase 5: Testing (1 day)
- [ ] Unit tests for DeviceTools
- [ ] Integration tests
- [ ] Manual end-to-end testing
- [ ] Performance tuning

---

## 10. Verification

After implementation, verify:

1. **Basic automation**: "Play Despacito on YouTube"
   - Agent launches YouTube
   - Searches for video
   - Plays it
   - Calls taskComplete()

2. **Error recovery**: "Open NonExistentApp"
   - Agent tries to launch
   - Gets failure feedback
   - Reports via taskFailed()

3. **Multi-step tasks**: "Send 'Hello' to John on WhatsApp"
   - Launches WhatsApp
   - Navigates to John
   - Types message
   - Sends it
   - Confirms completion

4. **Cancellation**: Start task, press cancel
   - Agent stops mid-execution
   - UI shows "Cancelled" state

---

## References

- [Koog Documentation](https://docs.koog.ai/)
- [Koog Basic Agents](https://docs.koog.ai/basic-agents/)
- [Koog Annotation-Based Tools](https://docs.koog.ai/annotation-based-tools/)
- [Koog Agent Events](https://docs.koog.ai/agent-events/)
- [Koog GitHub](https://github.com/JetBrains/koog)