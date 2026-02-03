package com.aiassistant.presentation.chat

import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.markdown.markdown
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
                currentTool = null,
                stepCount = 0
            )
        }
        _sideEffect.trySend(ChatSideEffect.ScrollToBottom)

        executionJob = viewModelScope.launch {
            try {
                val config = AgentConfig(
                    provider = LLMProvider.OPENAI,
                    apiKey = apiKeyProvider.getApiKey()
                )

                val processor = AgentEventProcessor(_isAgentOpen)


                val executor = simpleOpenAIExecutor(apiKeyProvider.getApiKey())

                // Collect progress for UI updates
                val progressJob = launch {
                    processor.progress.collect { progress ->
                        handleProgress(progress)
                    }
                }


                val agent = agentFactory.createAgent(config).apply {
                    //install(Tracing) { addMessageProcessor(processor) }
                }

                val result = agent.run(command)



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
                _sideEffect.send(ChatSideEffect.ScrollToBottom)

            } catch (e: kotlinx.coroutines.CancellationException) {
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
                _state.update {
                    it.copy(
                        isExecuting = false,
                        currentTool = null
                    )
                }
                _sideEffect.send(ChatSideEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun handleProgress(progress: AgentProgress) {
        when (progress) {
            is AgentProgress.Started -> {
                _state.update {
                    it.copy(currentStep = "Agent started", stepCount = 0)
                }
            }
            is AgentProgress.ToolExecuting -> {
                _state.update {
                    it.copy(
                        currentStep = "Executing: ${progress.toolName}",
                        currentTool = progress.toolName,
                        stepCount = it.stepCount + 1
                    )
                }
            }
            is AgentProgress.Completed -> {
                _state.update {
                    it.copy(currentStep = "Completed", currentTool = null)
                }
            }
            is AgentProgress.Failed -> {
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
        executionJob?.cancel()
        _state.update {
            it.copy(
                isExecuting = false,
                currentStep = "Cancelled",
                currentTool = null
            )
        }
    }
}
