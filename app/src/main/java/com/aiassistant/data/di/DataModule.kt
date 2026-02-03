package com.aiassistant.data.di

import android.content.Context
import androidx.room.Room
import com.aiassistant.data.local.AppDatabase
import com.aiassistant.data.local.dao.TaskLogDao
import com.aiassistant.data.repository.AppRepositoryImpl
import com.aiassistant.data.repository.ScreenRepositoryImpl
import com.aiassistant.data.repository.TaskLogRepositoryImpl
import com.aiassistant.domain.repository.AppRepository
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.repository.TaskLogRepository
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
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "clawdroid_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTaskLogDao(database: AppDatabase): TaskLogDao {
        return database.taskLogDao()
    }
}
