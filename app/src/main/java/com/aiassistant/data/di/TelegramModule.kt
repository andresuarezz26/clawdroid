package com.aiassistant.data.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aiassistant.data.remote.telegram.TelegramApi
import com.aiassistant.data.remote.telegram.TelegramApiImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

interface TelegramTokenProvider {
    fun getToken(): String?
    fun setToken(token: String)
    fun clearToken()
    fun hasToken(): Boolean
}

@Module
@InstallIn(SingletonComponent::class)
object TelegramModule {

    @Provides
    @Singleton
    fun provideTelegramTokenProvider(@ApplicationContext context: Context): TelegramTokenProvider {
        return TelegramTokenProviderImpl(context)
    }

    @Provides
    @Singleton
    fun provideTelegramApi(impl: TelegramApiImpl): TelegramApi = impl
}

class TelegramTokenProviderImpl(context: Context) : TelegramTokenProvider {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "clawdroid_telegram_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("clawdroid_telegram_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    override fun getToken(): String? {
        val token = prefs.getString(TELEGRAM_TOKEN_PREF, null)
        return if (token.isNullOrBlank()) null else token
    }

    override fun setToken(token: String) {
        prefs.edit().putString(TELEGRAM_TOKEN_PREF, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(TELEGRAM_TOKEN_PREF).apply()
    }

    override fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    companion object {
        private const val TELEGRAM_TOKEN_PREF = "telegram_bot_token"
    }
}
