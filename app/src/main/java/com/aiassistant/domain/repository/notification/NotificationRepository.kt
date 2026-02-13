package com.aiassistant.domain.repository.notification

import com.aiassistant.domain.model.DeviceNotification
import kotlinx.coroutines.flow.SharedFlow

interface NotificationRepository {
    val incomingNotifications: SharedFlow<DeviceNotification>
    suspend fun saveNotification(notification: DeviceNotification)
    suspend fun getRecentNotifications(limit: Int = 50, packageFilter: String? = null): List<DeviceNotification>
    suspend fun clearOldNotifications(olderThanMs: Long)
}
