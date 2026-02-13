package com.aiassistant.domain.model

data class DeviceNotification(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val isOngoing: Boolean,
    val category: String?
)
