package com.aiassistant.framework.notification

import android.app.PendingIntent
import android.app.RemoteInput
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationReplyAction(
    val pendingIntent: PendingIntent,
    val remoteInputs: List<RemoteInput>,
    val notificationKey: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis()
)

private const val MAX_ENTRIES = 50

@Singleton
class NotificationActionStore @Inject constructor() {

    private val actions = object : LinkedHashMap<String, NotificationReplyAction>(
        MAX_ENTRIES, 0.75f, false
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, NotificationReplyAction>?
        ): Boolean = size > MAX_ENTRIES
    }

    @Synchronized
    fun storeReplyAction(key: String, action: NotificationReplyAction) {
        actions[key] = action
    }

    @Synchronized
    fun getReplyAction(key: String): NotificationReplyAction? = actions[key]

    @Synchronized
    fun removeReplyAction(key: String) {
        actions.remove(key)
    }

    @Synchronized
    fun evictOlderThan(maxAgeMs: Long) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        actions.values.removeAll { it.timestamp < cutoff }
    }
}
