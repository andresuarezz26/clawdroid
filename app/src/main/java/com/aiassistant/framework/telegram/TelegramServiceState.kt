package com.aiassistant.framework.telegram

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class TelegramServiceState @Inject constructor() {

  private val _isRunning = MutableStateFlow(false)
  val isRunning = _isRunning.asStateFlow()

  fun setRunning(running: Boolean) {
    _isRunning.value = running
  }
}