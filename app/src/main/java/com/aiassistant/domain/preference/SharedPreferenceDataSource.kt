package com.aiassistant.domain.preference

interface SharedPreferenceDataSource {
  fun getToken(): String?
  fun setToken(token: String)
  fun clearToken()
  fun hasToken(): Boolean
}