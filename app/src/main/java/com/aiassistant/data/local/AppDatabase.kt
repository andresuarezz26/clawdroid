package com.aiassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aiassistant.data.local.dao.notification.NotificationDao
import com.aiassistant.data.local.dao.telegram.ConversationDao
import com.aiassistant.data.local.dao.telegram.MessageDao
import com.aiassistant.data.local.entity.notification.NotificationEntity
import com.aiassistant.data.local.entity.ConversationEntity
import com.aiassistant.data.local.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        NotificationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun telegramConversationDao(): ConversationDao
    abstract fun telegramMessageDao(): MessageDao
    abstract fun notificationDao(): NotificationDao
}