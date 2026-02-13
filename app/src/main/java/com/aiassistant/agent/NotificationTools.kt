package com.aiassistant.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import android.util.Log
import com.aiassistant.domain.usecase.notification.GetRecentNotificationsUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "Agent"

@LLMDescription("Tools for querying device notifications")
class NotificationTools @Inject constructor(
    private val getRecentNotificationsUseCase: GetRecentNotificationsUseCase
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
}
