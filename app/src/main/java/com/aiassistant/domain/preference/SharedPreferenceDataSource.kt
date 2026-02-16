package com.aiassistant.domain.preference

interface SharedPreferenceDataSource {
  fun getToken(): String?
  fun setToken(token: String)
  fun clearToken()
  fun hasToken(): Boolean
  fun getNotificationForwardingEnabled(): Boolean
  fun setNotificationForwardingEnabled(enabled: Boolean)
  fun getNotificationFilterPackages(): Set<String>
  fun setNotificationFilterPackages(packages: Set<String>)
  fun getTelegramChatId(): Long?
  fun setTelegramChatId(chatId: Long)
}