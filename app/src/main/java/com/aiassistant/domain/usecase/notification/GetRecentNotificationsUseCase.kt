package com.aiassistant.domain.usecase.notification

import com.aiassistant.domain.model.DeviceNotification
import com.aiassistant.domain.repository.notification.NotificationRepository
import javax.inject.Inject

class GetRecentNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(limit: Int = 50, packageFilter: String? = null): List<DeviceNotification> {
        return repository.getRecentNotifications(limit, packageFilter)
    }
}
