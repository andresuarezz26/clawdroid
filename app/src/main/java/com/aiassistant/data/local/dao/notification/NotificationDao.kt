package com.aiassistant.data.local.dao.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aiassistant.data.local.entity.notification.NotificationEntity

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentByPackage(packageName: String, limit: Int): List<NotificationEntity>

    @Query("DELETE FROM notifications WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
