package com.aiassistant.data.repository.notification

import com.aiassistant.data.local.dao.notification.NotificationDao
import com.aiassistant.data.local.entity.notification.NotificationEntity
import com.aiassistant.domain.model.DeviceNotification
import com.aiassistant.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    private val _incomingNotifications = MutableSharedFlow<DeviceNotification>(extraBufferCapacity = 64)
    override val incomingNotifications: SharedFlow<DeviceNotification> = _incomingNotifications.asSharedFlow()

    override suspend fun saveNotification(notification: DeviceNotification) {
        val entity = NotificationEntity(
            packageName = notification.packageName,
            appName = notification.appName,
            title = notification.title,
            text = notification.text,
            timestamp = notification.timestamp,
            isOngoing = notification.isOngoing,
            category = notification.category
        )
        notificationDao.insert(entity)
        _incomingNotifications.tryEmit(notification)
    }

    override suspend fun getRecentNotifications(limit: Int, packageFilter: String?): List<DeviceNotification> {
        val entities = if (packageFilter != null) {
            notificationDao.getRecentByPackage(packageFilter, limit)
        } else {
            notificationDao.getRecent(limit)
        }
        return entities.map { it.toDomain() }
    }

    override suspend fun clearOldNotifications(olderThanMs: Long) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        notificationDao.deleteOlderThan(cutoff)
    }

    private fun NotificationEntity.toDomain() = DeviceNotification(
        id = id,
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        timestamp = timestamp,
        isOngoing = isOngoing,
        category = category
    )
}
