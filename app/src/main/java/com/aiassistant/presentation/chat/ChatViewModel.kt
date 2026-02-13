package com.aiassistant.presentation.chat

import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.markdown.markdown
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiassistant.agent.AgentConfig
import com.aiassistant.agent.AgentEventProcessor
import com.aiassistant.agent.AgentProgress
import com.aiassistant.agent.AndroidAgentFactory
import com.aiassistant.agent.LLMProvider
import com.aiassistant.data.remote.ApiKeyProvider
import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.repository.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.isActive
import kotlinx.io.files.Path

private const val TAG = "Agent"

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentFactory: AndroidAgentFactory,
    private val apiKeyProvider: ApiKeyProvider,
    private val screenRepository: ScreenRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _isAgentOpen = MutableStateFlow(true)

    private val _sideEffect = Channel<ChatSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<ChatSideEffect> = _sideEffect.receiveAsFlow()

    private var executionJob: Job? = null

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
        Log.i(TAG, "executeCommand called with: $command")
        if (command.isBlank()) {
            Log.i(TAG, "Command is blank, returning")
            return
        }
        if (!_state.value.isServiceConnected) {
            Log.i(TAG, "Service not connected, opening accessibility settings")
            viewModelScope.launch {
                _sideEffect.send(ChatSideEffect.OpenAccessibilitySettings)
            }
            return
        }
        if (!_state.value.isServiceConnected) {
            Log.i(TAG, "Service not connected — quick actions still available, UI automation disabled")
        }

        Log.i(TAG, "Starting command execution")

        // Capture conversation history BEFORE adding the new user message
        val historyForContext = _state.value.messages

        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(content = command, isUser = true),
                inputText = "",
                isExecuting = true,
                currentStep = "Starting...",
                currentTool = null,
                stepCount = 0
            )
        }
        _sideEffect.trySend(ChatSideEffect.ScrollToBottom)

        executionJob = viewModelScope.launch {
            try {
                Log.i(TAG, "Creating agent config")
                val config = AgentConfig(
                    provider = LLMProvider.OPENAI,
                    apiKey = apiKeyProvider.getApiKey()
                )

                val processor = AgentEventProcessor(_isAgentOpen)

                // Collect progress for UI updates
                val progressJob = launch {
                    processor.progress.collect { progress ->
                        handleProgress(progress)
                    }
                }

                Log.i(TAG, "Creating agent with ${historyForContext.size} messages of conversation history")
                val agent = agentFactory.createAgent(config, historyForContext).apply {
                    //install(Tracing) { addMessageProcessor(processor) }
                }

                Log.i(TAG, "Running agent with command: $command")
                val result = runWithRetry(maxRetries = 3) {
                    agent.run(command)
                }

                Log.i(TAG, "Agent run completed with result: $result")



                progressJob.cancel()


                // Parse the result to determine success/failure
                val resultText = when {
                    result.contains("TASK_COMPLETE:") -> {
                        "Done! ${result.substringAfter("TASK_COMPLETE:").trim()}"
                    }
                    result.contains("TASK_FAILED:") -> {
                        "Failed: ${result.substringAfter("TASK_FAILED:").trim()}"
                    }
                    else -> result
                }

                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            content = resultText,
                            isUser = false
                        ),
                        isExecuting = false,
                        currentTool = null
                    )
                }
                _sideEffect.send(ChatSideEffect.BringToForeground)
                _sideEffect.send(ChatSideEffect.ScrollToBottom)

            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.i(TAG, "Agent execution cancelled")
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            content = "Task cancelled.",
                            isUser = false
                        ),
                        isExecuting = false,
                        currentTool = null
                    )
                }
            } catch (e: Exception) {
                Log.i(TAG, "Agent execution error: ${e.message}", e)
                _state.update {
                    it.copy(
                        isExecuting = false,
                        currentTool = null
                    )
                }
                _sideEffect.send(ChatSideEffect.BringToForeground)
                _sideEffect.send(ChatSideEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun handleProgress(progress: AgentProgress) {
        Log.i(TAG, "handleProgress: $progress")
        when (progress) {
            is AgentProgress.Started -> {
                Log.i(TAG, "Progress: Agent started")
                _state.update {
                    it.copy(currentStep = "Agent started", stepCount = 0)
                }
            }
            is AgentProgress.ToolExecuting -> {
                Log.i(TAG, "Progress: Executing tool ${progress.toolName}")
                _state.update {
                    it.copy(
                        currentStep = "Executing: ${progress.toolName}",
                        currentTool = progress.toolName,
                        stepCount = it.stepCount + 1
                    )
                }
            }
            is AgentProgress.Completed -> {
                Log.i(TAG, "Progress: Completed")
                _state.update {
                    it.copy(currentStep = "Completed", currentTool = null)
                }
            }
            is AgentProgress.Failed -> {
                Log.i(TAG, "Progress: Failed with error ${progress.error}")
                _state.update {
                    it.copy(
                        currentStep = "Failed: ${progress.error}",
                        currentTool = null
                    )
                }
            }
        }
    }

    private fun cancelExecution() {
        Log.i(TAG, "cancelExecution called")
        executionJob?.cancel()
        _state.update {
            it.copy(
                isExecuting = false,
                currentStep = "Cancelled",
                currentTool = null
            )
        }
    }

    /**
     * Retry logic for transient network errors (5xx, timeouts).
     * Uses exponential backoff: 1s, 2s, 4s delays between retries.
     */
    private suspend fun <T> runWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var currentDelay = initialDelayMs

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // Don't retry cancellation
            } catch (e: Exception) {
                val isTransient = e.message?.let { msg ->
                    msg.contains("502") || msg.contains("503") || msg.contains("504") ||
                    msg.contains("Bad Gateway") || msg.contains("Service Unavailable") ||
                    msg.contains("Gateway Timeout") || msg.contains("timeout", ignoreCase = true) ||
                    msg.contains("prematurely closed", ignoreCase = true) ||
                    msg.contains("EOFException", ignoreCase = true) ||
                    msg.contains("connection reset", ignoreCase = true)
                } ?: (e is java.io.EOFException)

                if (isTransient && attempt < maxRetries - 1) {
                    Log.w(TAG, "Transient error on attempt ${attempt + 1}, retrying in ${currentDelay}ms: ${e.message}")
                    delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                    lastException = e
                } else {
                    throw e // Non-transient error or last attempt, propagate
                }
            }
        }

        throw lastException ?: IllegalStateException("Retry failed without exception")
    }
}
