package com.aiassistant.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.aiassistant.domain.usecase.notification.GetRecentNotificationsUseCase
import com.aiassistant.framework.notification.NotificationActionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "Agent"

@LLMDescription("Tools for querying device notifications")
class NotificationTools @Inject constructor(
    private val getRecentNotificationsUseCase: GetRecentNotificationsUseCase,
    private val notificationActionStore: NotificationActionStore,
    @ApplicationContext private val context: Context
) : ToolSet {

    @Tool
    @LLMDescription("Get recent device notifications. Returns a formatted list of recent notifications received on the device. Use this to check what notifications the user has received.")
    suspend fun getRecentNotifications(
        @LLMDescription("Maximum number of notifications to return (default 20)") limit: Int = 20,
        @LLMDescription("Optional app package name to filter by (e.g. 'com.whatsapp'). Empty string for all apps.") appPackage: String = ""
    ): String {
        Log.i(TAG, "Tool: getRecentNotifications(limit=$limit, appPackage='$appPackage') called")
        val packageFilter = appPackage.ifBlank { null }
        val notifications = getRecentNotificationsUseCase(limit, packageFilter)

        if (notifications.isEmpty()) {
            val result = if (packageFilter != null) {
                "No recent notifications from $packageFilter"
            } else {
                "No recent notifications"
            }
            Log.i(TAG, "Tool: getRecentNotifications() -> $result")
            return result
        }

        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val result = notifications.joinToString("\n") { notification ->
            val time = formatter.format(Date(notification.timestamp))
            val title = notification.title ?: ""
            val text = notification.text ?: ""
            val content = when {
                title.isNotBlank() && text.isNotBlank() -> "$title - $text"
                title.isNotBlank() -> title
                else -> text
            }
            "[$time] ${notification.appName}: $content"
        }

        Log.i(TAG, "Tool: getRecentNotifications() -> ${notifications.size} notifications")
        return result
    }

    @Tool
    @LLMDescription("Reply to a notification directly using its inline reply action. Instant, no app opening needed. Only works when the notification prompt includes 'ReplyCapable: YES'.")
    suspend fun replyToNotification(
        @LLMDescription("The notification key from the notification prompt") notificationKey: String,
        @LLMDescription("The text to reply with") replyText: String
    ): String {
        Log.i(TAG, "Tool: replyToNotification(key=$notificationKey, text='$replyText') called")

        val action = notificationActionStore.getReplyAction(notificationKey)
            ?: return "FAILED: No reply action found for notification key '$notificationKey'. The notification may have been dismissed or does not support inline reply.".also {
                Log.w(TAG, it)
            }

        return try {
            val intent = Intent()
            val bundle = Bundle()
            for (remoteInput in action.remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            android.app.RemoteInput.addResultsToIntent(
                action.remoteInputs.toTypedArray(),
                intent,
                bundle
            )
            action.pendingIntent.send(context, 0, intent)
            notificationActionStore.removeReplyAction(notificationKey)
            Log.i(TAG, "Tool: replyToNotification() -> Success, replied to ${action.packageName}")
            "Reply sent successfully to ${action.packageName}."
        } catch (e: PendingIntent.CanceledException) {
            notificationActionStore.removeReplyAction(notificationKey)
            Log.e(TAG, "PendingIntent was cancelled", e)
            "FAILED: The notification's reply action has expired or been cancelled. Try opening the app manually."
        }
    }
}
