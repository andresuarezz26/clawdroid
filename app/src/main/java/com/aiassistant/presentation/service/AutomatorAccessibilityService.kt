package com.aiassistant.presentation.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AutomatorAccessibilityService : AccessibilityService() {

  companion object {
    var instance: AutomatorAccessibilityService? = null
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
  }

  override fun onDestroy() {
    instance = null
    super.onDestroy()
  }

  override fun onAccessibilityEvent(p0: AccessibilityEvent?) {

  }

  override fun onInterrupt() {

  }
}