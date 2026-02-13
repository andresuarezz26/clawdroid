package com.aiassistant.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aiassistant.domain.preference.SharedPreferenceDataSource
import javax.inject.Inject

class SharedPreferenceDataSourceImpl @Inject constructor(context: Context) :
  SharedPreferenceDataSource {
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
    prefs.edit { putString(TELEGRAM_TOKEN_PREF, token) }
  }

  override fun clearToken() {
    prefs.edit { remove(TELEGRAM_TOKEN_PREF) }
  }

  override fun hasToken(): Boolean {
    return !getToken().isNullOrBlank()
  }

  companion object {
    private const val TELEGRAM_TOKEN_PREF = "telegram_bot_token"
  }
}