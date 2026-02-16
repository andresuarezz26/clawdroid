package com.aiassistant.framework.telegram

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.aiassistant.agent.AgentExecutor
import com.aiassistant.agent.AgentResult
import com.aiassistant.domain.model.DEFAULT_CHAT_ID
import com.aiassistant.domain.preference.SharedPreferenceDataSource
import com.aiassistant.domain.usecase.messages.GetConversationHistoryUseCase
import com.aiassistant.domain.usecase.telegram.PollUpdatesTelegramUseCase
import com.aiassistant.domain.usecase.messages.SaveMessageUseCase
import com.aiassistant.domain.usecase.telegram.SendResponseTelegramUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TelegramBotService"

@AndroidEntryPoint
class TelegramBotService : Service() {

    @Inject
    lateinit var pollUpdatesTelegramUseCase: PollUpdatesTelegramUseCase

    @Inject
    lateinit var sendResponseTelegramUseCase: SendResponseTelegramUseCase

    @Inject
    lateinit var saveMessageUseCase: SaveMessageUseCase

    @Inject
    lateinit var getConversationHistoryUseCase: GetConversationHistoryUseCase

    @Inject
    lateinit var agentExecutor: AgentExecutor

    @Inject
    lateinit var notificationManager: TelegramNotificationManager

    @Inject
    lateinit var serviceState: TelegramServiceState

    @Inject
    lateinit var preferences: SharedPreferenceDataSource

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var lastUpdateOffset: Long? = null
    private var messageCount = 0

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")

        serviceState.setRunning(true)

        val notification = notificationManager.createServiceNotification("Starting...")
        startForeground(TelegramNotificationManager.NOTIFICATION_ID, notification)

        startPolling()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceState.setRunning(false)
        Log.i(TAG, "Service destroyed")
        stopPolling()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) {
            Log.i(TAG, "Polling already active")
            return
        }

        pollingJob = serviceScope.launch {
            var retryDelay = 1_000L

            while (isActive) {
                try {
                    Log.d(TAG, "Polling for updates, offset: $lastUpdateOffset")

                    val result = pollUpdatesTelegramUseCase(lastUpdateOffset)

                    result.onSuccess { updates ->
                        retryDelay = 1_000L // Reset on success

                        if (updates.isNotEmpty()) {
                            Log.i(TAG, "Received ${updates.size} updates")
                            lastUpdateOffset = updates.maxOf { it.updateId } + 1

                            for (update in updates) {
                                processMessage(update.chatId, update.text, update.username, update.firstName)
                            }
                        }

                        updateNotification()
                    }.onFailure { error ->
                        Log.e(TAG, "Polling error: ${error.message}")
                        notificationManager.updateNotification("Error: ${error.message?.take(30)}")

                        delay(retryDelay)
                        retryDelay = (retryDelay * 2).coerceAtMost(30_000L)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error in polling loop", e)
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(30_000L)
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun processMessage(chatId: Long, text: String, username: String?, firstName: String?) {
        Log.i(TAG, "Processing message from $chatId: $text")

        try {
            // Save user message to unified conversation
            saveMessageUseCase(DEFAULT_CHAT_ID, text, isFromUser = true)
            messageCount++

            // Get conversation history from unified conversation
            val history = getConversationHistoryUseCase(DEFAULT_CHAT_ID, limit = 20)

            // Execute agent (don't require service connection for Telegram - we allow text responses)
            val result = agentExecutor.execute(
                command = text,
                conversationHistory = history,
                requireServiceConnection = false
            )

            // Prepare response based on result
            val responseText = when (result) {
                is AgentResult.Success -> result.response
                is AgentResult.Failure -> "Error: ${result.reason}"
                is AgentResult.ServiceNotConnected -> {
                    "The device automation service is not connected. Please enable it in Clawdroid settings."
                }
                is AgentResult.Cancelled -> "Task was cancelled."
            }

            // Save bot response to unified conversation
            saveMessageUseCase(DEFAULT_CHAT_ID, responseText, isFromUser = false)

            // Send response to Telegram (using real Telegram chatId)
            sendResponseTelegramUseCase(chatId, responseText).onFailure { error ->
                Log.e(TAG, "Failed to send response: ${error.message}")
            }

            updateNotification()

            // Persist telegram chat ID so NotificationReactor can send to Telegram independently
            preferences.setTelegramChatId(chatId)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing message", e)

            val errorMessage = "Sorry, an error occurred: ${e.message}"
            sendResponseTelegramUseCase(chatId, errorMessage)
        }
    }

    private fun updateNotification() {
        val statusText = "Running - $messageCount messages processed"
        notificationManager.updateNotification(statusText)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TelegramBotService::class.java)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
          }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TelegramBotService::class.java)
            context.stopService(intent)
        }
    }
}
