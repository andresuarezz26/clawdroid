package com.aiassistant.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aiassistant.data.local.AppDatabase
import com.aiassistant.data.local.dao.TaskLogDao
import com.aiassistant.data.local.dao.notification.NotificationDao
import com.aiassistant.data.local.dao.telegram.TelegramConversationDao
import com.aiassistant.data.local.dao.telegram.TelegramMessageDao
import com.aiassistant.data.repository.AppRepositoryImpl
import com.aiassistant.data.repository.ScreenRepositoryImpl
import com.aiassistant.data.repository.TaskLogRepositoryImpl
import com.aiassistant.data.repository.TelegramRepositoryImpl
import com.aiassistant.data.repository.notification.NotificationRepositoryImpl
import com.aiassistant.domain.repository.AppRepository
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.repository.TaskLogRepository
import com.aiassistant.domain.repository.notification.NotificationRepository
import com.aiassistant.domain.repository.telegram.TelegramRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindScreenRepository(impl: ScreenRepositoryImpl): ScreenRepository

    @Binds
    @Singleton
    abstract fun bindTaskLogRepository(impl: TaskLogRepositoryImpl): TaskLogRepository

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindTelegramRepository(impl: TelegramRepositoryImpl): TelegramRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    packageName TEXT NOT NULL,
                    appName TEXT NOT NULL,
                    title TEXT,
                    text TEXT,
                    timestamp INTEGER NOT NULL,
                    isOngoing INTEGER NOT NULL,
                    category TEXT
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_timestamp ON notifications(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_packageName ON notifications(packageName)")
        }
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS telegram_conversations (
                    chatId INTEGER PRIMARY KEY NOT NULL,
                    username TEXT,
                    firstName TEXT,
                    lastMessageAt INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS telegram_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    chatId INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    isFromUser INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY(chatId) REFERENCES telegram_conversations(chatId) ON DELETE CASCADE
                )
            """.trimIndent())

            db.execSQL("CREATE INDEX IF NOT EXISTS index_telegram_messages_chatId ON telegram_messages(chatId)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "clawdroid_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskLogDao(database: AppDatabase): TaskLogDao {
        return database.taskLogDao()
    }

    @Provides
    @Singleton
    fun provideTelegramConversationDao(database: AppDatabase): TelegramConversationDao {
        return database.telegramConversationDao()
    }

    @Provides
    @Singleton
    fun provideTelegramMessageDao(database: AppDatabase): TelegramMessageDao {
        return database.telegramMessageDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }
}
