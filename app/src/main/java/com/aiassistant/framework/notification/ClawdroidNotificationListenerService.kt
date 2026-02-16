package com.aiassistant.framework.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aiassistant.domain.model.DeviceNotification
import com.aiassistant.domain.repository.notification.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "NotificationListener"
private const val DEDUP_MAX_SIZE = 100
private const val DEDUP_TIME_BUCKET_MS = 2000L
private const val OLD_NOTIFICATION_THRESHOLD_MS = 24L * 60 * 60 * 1000

class ClawdroidNotificationListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationListenerEntryPoint {
        fun notificationRepository(): NotificationRepository
        fun notificationListenerServiceState(): NotificationListenerServiceState
        fun notificationReactor(): NotificationReactor
        fun notificationActionStore(): NotificationActionStore
    }

    private var repository: NotificationRepository? = null
    private var serviceStateTracker: NotificationListenerServiceState? = null
    private var notificationReactor: NotificationReactor? = null
    private var notificationActionStore: NotificationActionStore? = null
    private var scope: CoroutineScope? = null
    private val recentNotificationKeys = LinkedHashSet<String>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationListenerEntryPoint::class.java
        )
        repository = entryPoint.notificationRepository()
        serviceStateTracker = entryPoint.notificationListenerServiceState()
        notificationReactor = entryPoint.notificationReactor()
        notificationActionStore = entryPoint.notificationActionStore()

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceStateTracker?.setConnected(true)
        notificationReactor?.startReacting()

        notificationActionStore?.evictOlderThan(OLD_NOTIFICATION_THRESHOLD_MS)

        scope?.launch {
            try {
                repository?.clearOldNotifications(OLD_NOTIFICATION_THRESHOLD_MS)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clean old notifications", e)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "Notification listener disconnected")
        serviceStateTracker?.setConnected(false)
        notificationReactor?.stopReacting()
        scope?.cancel()
        scope = null
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        notificationActionStore?.removeReplyAction(sbn.key)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val repo = repository ?: return
        val currentScope = scope ?: return

        // Skip own package
        if (sbn.packageName == applicationContext.packageName) return

        // Skip ongoing notifications
        if (sbn.isOngoing) return

        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString()
        val text = extras?.getCharSequence("android.text")?.toString()

        // Skip empty notifications (no title and no text)
        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        // Dedup: key based on package|title|text|timeBucket
        val timeBucket = sbn.postTime / DEDUP_TIME_BUCKET_MS
        val dedupKey = "${sbn.packageName}|$title|$text|$timeBucket"
        if (recentNotificationKeys.contains(dedupKey)) return
        recentNotificationKeys.add(dedupKey)
        if (recentNotificationKeys.size > DEDUP_MAX_SIZE) {
            val iterator = recentNotificationKeys.iterator()
            iterator.next()
            iterator.remove()
        }

        val appName = try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        // Extract inline reply action if present
        val notifKey = sbn.key
        var hasReplyAction = false
        val actions = sbn.notification.actions
        if (actions != null) {
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                    notificationActionStore?.storeReplyAction(
                        notifKey,
                        NotificationReplyAction(
                            pendingIntent = action.actionIntent,
                            remoteInputs = remoteInputs.toList(),
                            notificationKey = notifKey,
                            packageName = sbn.packageName
                        )
                    )
                    hasReplyAction = true
                    Log.d(TAG, "Stored reply action for $notifKey from $appName")
                    break
                }
            }
        }

        val notification = DeviceNotification(
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            isOngoing = false,
            category = sbn.notification.category,
            notificationKey = notifKey,
            hasReplyAction = hasReplyAction
        )

        Log.d(TAG, "Notification from $appName: $title - $text (replyCapable=$hasReplyAction)")

        currentScope.launch {
            try {
                repo.saveNotification(notification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save notification", e)
            }
        }
    }
}
