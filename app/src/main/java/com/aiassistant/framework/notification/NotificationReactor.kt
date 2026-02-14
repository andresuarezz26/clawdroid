package com.aiassistant.framework.notification

import android.util.Log
import com.aiassistant.agent.AgentExecutor
import com.aiassistant.agent.AgentResult
import com.aiassistant.domain.model.DEFAULT_CHAT_ID
import com.aiassistant.domain.preference.SharedPreferenceDataSource
import com.aiassistant.domain.repository.notification.NotificationRepository
import com.aiassistant.domain.usecase.messages.GetConversationHistoryUseCase
import com.aiassistant.domain.usecase.messages.SaveMessageUseCase
import com.aiassistant.domain.usecase.telegram.SendResponseTelegramUseCase
import com.aiassistant.framework.telegram.TelegramServiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationReactor"
private const val RATE_LIMIT_MS = 5_000L

@Singleton
class NotificationReactor @Inject constructor(
    private val repository: NotificationRepository,
    private val agentExecutor: AgentExecutor,
    private val sendResponseTelegramUseCase: SendResponseTelegramUseCase,
    private val saveMessageUseCase: SaveMessageUseCase,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
    private val telegramServiceState: TelegramServiceState,
    private val preferences: SharedPreferenceDataSource
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null
    private var lastAgentExecutionTime = 0L

    fun startReacting(telegramChatId: Long) {
        if (collectionJob?.isActive == true) return

        Log.i(TAG, "Starting notification reactor for telegramChatId=$telegramChatId")
        collectionJob = scope.launch {
            repository.incomingNotifications.collect { notification ->
                try {
                    // Check if forwarding is enabled
                    if (!preferences.getNotificationForwardingEnabled()) return@collect

                    // Check if Telegram is running
                    if (!telegramServiceState.isRunning.value) return@collect

                    // Check package filter
                    val filterPackages = preferences.getNotificationFilterPackages()
                    if (filterPackages.isNotEmpty() && notification.packageName !in filterPackages) return@collect

                    // Rate limit
                    val now = System.currentTimeMillis()
                    if (now - lastAgentExecutionTime < RATE_LIMIT_MS) {
                        Log.d(TAG, "Rate limited, skipping notification from ${notification.appName}")
                        return@collect
                    }
                    lastAgentExecutionTime = now

                    val prompt = buildString {
                        append("[NOTIFICATION] ")
                        append("App: ${notification.appName}")
                        if (!notification.title.isNullOrBlank()) {
                            append(" | From: ${notification.title}")
                        }
                        if (!notification.text.isNullOrBlank()) {
                            append(" | Message: ${notification.text}")
                        }
                        append("\n\nYou received this notification on the device. ")
                        append("Briefly summarize what it is and if any action seems needed. ")
                        append("Keep your response short and informative.")
                    }

                    Log.i(TAG, "Processing notification from ${notification.appName}")

                    // Save as user message to unified conversation
                    saveMessageUseCase(DEFAULT_CHAT_ID, prompt, isFromUser = true)

                    // Get conversation history from unified conversation
                    val history = getConversationHistoryUseCase(DEFAULT_CHAT_ID, limit = 20)

                    // Execute agent
                    val result = agentExecutor.execute(
                        command = prompt,
                        conversationHistory = history,
                        requireServiceConnection = false
                    )

                    val responseText = when (result) {
                        is AgentResult.Success -> result.response
                        is AgentResult.Failure -> "Could not process notification: ${result.reason}"
                        is AgentResult.ServiceNotConnected -> "Notification from ${notification.appName}: ${notification.title} - ${notification.text}"
                        is AgentResult.Cancelled -> return@collect
                    }

                    // Save response to unified conversation
                    saveMessageUseCase(DEFAULT_CHAT_ID, responseText, isFromUser = false)

                    // Send to Telegram (using real Telegram chatId)
                    sendResponseTelegramUseCase(telegramChatId, responseText).onFailure { error ->
                        Log.e(TAG, "Failed to send notification response to Telegram", error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification", e)
                }
            }
        }
    }

    fun stopReacting() {
        Log.i(TAG, "Stopping notification reactor")
        collectionJob?.cancel()
        collectionJob = null
    }
}
