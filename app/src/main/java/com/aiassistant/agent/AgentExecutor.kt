package com.aiassistant.agent

import android.util.Log
import com.aiassistant.data.remote.ApiKeyProvider
import com.aiassistant.domain.model.ChatMessage
import com.aiassistant.domain.repository.ScreenRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AgentExecutor"

@Singleton
class AgentExecutor @Inject constructor(
    private val agentFactory: AndroidAgentFactory,
    private val apiKeyProvider: ApiKeyProvider,
    private val screenRepository: ScreenRepository
) {
    private val _progress = MutableSharedFlow<AgentProgress>(replay = 1)
    val progress: SharedFlow<AgentProgress> = _progress.asSharedFlow()

    suspend fun execute(
        command: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        requireServiceConnection: Boolean = true
    ): AgentResult {
        Log.i(TAG, "execute called with: $command, historySize=${conversationHistory.size}")

        if (command.isBlank()) {
            return AgentResult.Failure("Command is blank")
        }

        if (requireServiceConnection && !screenRepository.isServiceConnected()) {
            Log.i(TAG, "Service not connected")
            return AgentResult.ServiceNotConnected
        }

        return try {
            _progress.emit(AgentProgress.Started)

            val config = AgentConfig(
                provider = LLMProvider.OPENAI,
                apiKey = apiKeyProvider.getApiKey()
            )

            Log.i(TAG, "Creating agent with ${conversationHistory.size} messages of conversation history")
            val agent = agentFactory.createAgent(config, conversationHistory)

            Log.i(TAG, "Running agent with command: $command")
            val result = runWithRetry(maxRetries = 3) {
                agent.run(command)
            }

            Log.i(TAG, "Agent run completed with result: $result")
            _progress.emit(AgentProgress.Completed(result))

            // Parse the result to determine success/failure
            val responseText = when {
                result.contains("TASK_COMPLETE:") -> {
                    "Done! ${result.substringAfter("TASK_COMPLETE:").trim()}"
                }
                result.contains("TASK_FAILED:") -> {
                    result.substringAfter("TASK_FAILED:").trim()
                }
                else -> result
            }

            if (result.contains("TASK_FAILED:")) {
                AgentResult.Failure(responseText)
            } else {
                AgentResult.Success(responseText)
            }

        } catch (e: CancellationException) {
            Log.i(TAG, "Agent execution cancelled")
            _progress.emit(AgentProgress.Failed("Cancelled"))
            AgentResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Agent execution error: ${e.message}", e)
            _progress.emit(AgentProgress.Failed(e.message))
            AgentResult.Failure(e.message ?: "Unknown error")
        }
    }

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val isTransient = e.message?.let { msg ->
                    msg.contains("502") || msg.contains("503") || msg.contains("504") ||
                    msg.contains("Bad Gateway") || msg.contains("Service Unavailable") ||
                    msg.contains("Gateway Timeout") || msg.contains("timeout", ignoreCase = true)
                } ?: false

                if (isTransient && attempt < maxRetries - 1) {
                    Log.w(TAG, "Transient error on attempt ${attempt + 1}, retrying in ${currentDelay}ms: ${e.message}")
                    delay(currentDelay)
                    currentDelay *= 2
                    lastException = e
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: IllegalStateException("Retry failed without exception")
    }
}
