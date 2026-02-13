package com.aiassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aiassistant.data.local.dao.TaskLogDao
import com.aiassistant.data.local.dao.notification.NotificationDao
import com.aiassistant.data.local.dao.telegram.TelegramConversationDao
import com.aiassistant.data.local.dao.telegram.TelegramMessageDao
import com.aiassistant.data.local.entity.TaskLogEntity
import com.aiassistant.data.local.entity.notification.NotificationEntity
import com.aiassistant.data.local.entity.telegram.TelegramConversationEntity
import com.aiassistant.data.local.entity.telegram.TelegramMessageEntity

@Database(
    entities = [
        TaskLogEntity::class,
        TelegramConversationEntity::class,
        TelegramMessageEntity::class,
        NotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskLogDao(): TaskLogDao
    abstract fun telegramConversationDao(): TelegramConversationDao
    abstract fun telegramMessageDao(): TelegramMessageDao
    abstract fun notificationDao(): NotificationDao
}