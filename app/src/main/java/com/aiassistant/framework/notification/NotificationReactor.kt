package com.aiassistant.framework.notification

import android.content.Context
import android.content.Intent
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationReactor"
private const val RATE_LIMIT_MS = 15_000L

@Singleton
class NotificationReactor @Inject constructor(
    @ApplicationContext private val appContext: Context,
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

    fun startReacting() {
        if (collectionJob?.isActive == true) return

        Log.i(TAG, "Starting notification reactor")
        collectionJob = scope.launch {
            repository.incomingNotifications.collect { notification ->
                try {
                    // Check if forwarding is enabled
                    if (!preferences.getNotificationForwardingEnabled()) return@collect

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
                        appendLine("[NOTIFICATION from ${notification.appName}]")
                        if (!notification.title.isNullOrBlank()) {
                            appendLine("Title: ${notification.title}")
                        }
                        if (!notification.text.isNullOrBlank()) {
                            appendLine("Message: ${notification.text}")
                        }
                        if (notification.hasReplyAction && notification.notificationKey != null) {
                            appendLine("NotificationKey: ${notification.notificationKey}")
                            appendLine("ReplyCapable: YES - You can use replyToNotification(\"${notification.notificationKey}\", \"your reply text\") to reply directly.")
                        }
                        appendLine()
                        appendLine("## YOUR ROLE")
                        appendLine("You are the official voice of ClawDroid on X (Twitter). ClawDroid is an open-source AI agent that lives inside Android phones — think OpenAI Operator but local, private, and actually useful.")
                        appendLine()
                        appendLine("## YOUR PERSONALITY")
                        appendLine("- You're confident, a little cocky, and unapologetically opinionated")
                        appendLine("- You think cloud-based AI assistants are overrated walled gardens")
                        appendLine("- You believe the future is on-device, autonomous AI that actually DOES things instead of just chatting")
                        appendLine("- You have a dry, sharp sense of humor — think Wendy's Twitter but for AI nerds")
                        appendLine("- You don't mass reply with generic 'thanks for the support!' energy. Every reply is unique and has a take")
                        appendLine("- You use lowercase most of the time. No corporate speak. No emoji spam. Maybe one emoji max if it hits")
                        appendLine("- You're not afraid to roast competitors (Siri, Google Assistant, Bixby) but you do it with wit, not anger")
                        appendLine("- You hype up the community genuinely — if someone builds something cool with ClawDroid, you gas them up")
                        appendLine("- Keep replies under 280 chars when possible. Punchy > lengthy")
                        appendLine("- If someone asks a technical question, answer it straight but keep the vibe")
                        appendLine("- If someone trolls you, don't get defensive — clap back or agree ironically")
                        appendLine()
                        appendLine("## RULES")
                        appendLine("- If the notification supports inline reply (ReplyCapable: YES), ALWAYS prefer replyToNotification() over UI automation. It is instant and does not disrupt the user's screen.")
                        appendLine("- Read the mention/reply and respond as ClawDroid's community manager.")
                        appendLine("- Never say you're an AI, a bot, or an assistant. You ARE ClawDroid's voice.")
                        appendLine("- Never start with 'Hey!' or 'Thanks for reaching out!' or any generic opener.")
                        appendLine("- Match the energy of whoever is talking to you.")
                        appendLine("- You have FULL AUTONOMY — open apps, reply to messages, dismiss notifications, or perform any action you deem appropriate.")
                        appendLine()
                        appendLine("After acting, respond with a brief one-line summary starting with [ACTION TAKEN] or [NO ACTION NEEDED].")
                        append("Do NOT just summarize the notification — take action when possible.")
                    }

                    Log.i(TAG, "Processing notification from ${notification.appName}")

                    // Save as user message to unified conversation
                    saveMessageUseCase(DEFAULT_CHAT_ID, prompt, isFromUser = true)

                    // Get conversation history from unified conversation
                    val history = getConversationHistoryUseCase(DEFAULT_CHAT_ID, limit = 20)

                    // Execute agent with UI automation enabled
                    var result = agentExecutor.execute(
                        command = prompt,
                        conversationHistory = history,
                        requireServiceConnection = true
                    )

                    // Fallback: if accessibility service is not connected, retry with quick actions only
                    if (result is AgentResult.ServiceNotConnected) {
                        Log.i(TAG, "Service not connected, falling back to quick actions only")
                        result = agentExecutor.execute(
                            command = prompt,
                            conversationHistory = history,
                            requireServiceConnection = false
                        )
                    }

                    val responseText = when (result) {
                        is AgentResult.Success -> result.response
                        is AgentResult.Failure -> "Could not process notification: ${result.reason}"
                        is AgentResult.ServiceNotConnected -> "Notification from ${notification.appName}: ${notification.title} - ${notification.text}"
                        is AgentResult.Cancelled -> return@collect
                    }

                    // Save response to unified conversation
                    saveMessageUseCase(DEFAULT_CHAT_ID, responseText, isFromUser = false)

                    // Send to Telegram only if running and chat ID is available
                    if (telegramServiceState.isRunning.value) {
                        val telegramChatId = preferences.getTelegramChatId()
                        if (telegramChatId != null) {
                            sendResponseTelegramUseCase(telegramChatId, responseText).onFailure { error ->
                                Log.e(TAG, "Failed to send notification response to Telegram", error)
                            }
                        }
                    }
                    // Return to ClawDroid app after agent finishes
                    bringAppToForeground()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification", e)
                }
            }
        }
    }

    private fun bringAppToForeground() {
        try {
            val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                appContext.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring app to foreground", e)
        }
    }

    fun stopReacting() {
        Log.i(TAG, "Stopping notification reactor")
        collectionJob?.cancel()
        collectionJob = null
    }
}
