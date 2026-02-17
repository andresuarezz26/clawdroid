package com.aiassistant.agent.di

import android.content.Context
import com.aiassistant.agent.AndroidAgentFactory
import com.aiassistant.agent.MobileAutomationTools
import com.aiassistant.agent.NotificationTools
import com.aiassistant.agent.QuickActionTools
import com.aiassistant.agent.RecurringTaskTools
import com.aiassistant.data.mapper.UINodeFormatter
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import com.aiassistant.domain.usecase.notification.GetRecentNotificationsUseCase
import com.aiassistant.framework.notification.NotificationActionStore
import com.aiassistant.framework.scheduler.RecurringTaskCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgentModule {

    @Provides
    @Singleton
    fun provideDeviceTools(
        screenRepository: ScreenRepository,
        uiNodeFormatter: UINodeFormatter
    ): MobileAutomationTools {
        return MobileAutomationTools(screenRepository, uiNodeFormatter)
    }

    @Provides
    @Singleton
    fun provideQuickActionTools(
        screenRepository: ScreenRepository
    ): QuickActionTools {
        return QuickActionTools(screenRepository)
    }

    @Provides
    @Singleton
    fun provideNotificationTools(
        getRecentNotificationsUseCase: GetRecentNotificationsUseCase,
        notificationActionStore: NotificationActionStore,
        @ApplicationContext context: Context
    ): NotificationTools {
        return NotificationTools(getRecentNotificationsUseCase, notificationActionStore, context)
    }

    @Provides
    @Singleton
    fun provideRecurringTaskTools(
        repository: RecurringTaskRepository,
        coordinator: RecurringTaskCoordinator
    ): RecurringTaskTools {
        return RecurringTaskTools(repository, coordinator)
    }

    @Provides
    @Singleton
    fun provideAndroidAgentFactory(
        mobileAutomationTools: MobileAutomationTools,
        quickActionTools: QuickActionTools,
        notificationTools: NotificationTools,
        recurringTaskTools: RecurringTaskTools
    ): AndroidAgentFactory {
        return AndroidAgentFactory(mobileAutomationTools, quickActionTools, notificationTools, recurringTaskTools)
    }
}
