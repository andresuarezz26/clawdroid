package com.aiassistant.data.di

import android.content.Context
import com.aiassistant.data.remote.telegram.TelegramApi
import com.aiassistant.data.remote.telegram.TelegramApiImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.aiassistant.data.local.preferences.SharedPreferenceDataSourceImpl
import com.aiassistant.domain.preference.SharedPreferenceDataSource

@Module
@InstallIn(SingletonComponent::class)
object TelegramModule {

    @Provides
    @Singleton
    fun provideSharedPreferenceDataSource(@ApplicationContext context: Context): SharedPreferenceDataSource {
        return SharedPreferenceDataSourceImpl(context)
    }

    @Provides
    @Singleton
    fun provideTelegramApi(impl: TelegramApiImpl): TelegramApi = impl
}

