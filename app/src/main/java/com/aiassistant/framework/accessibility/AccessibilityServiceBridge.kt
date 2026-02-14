package com.aiassistant.framework.accessibility

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

interface AccessibilityServiceBridge {
    fun getRootNode(): AccessibilityNodeInfo?
    fun isConnected(): StateFlow<Boolean>
    fun performGlobalAction(action: Int): Boolean
    fun getContext(): Context?
    fun dispatchGesture(gesture: GestureDescription, callback: GestureResultCallback?): Boolean
}

@Singleton
class AccessibilityServiceBridgeImpl @Inject constructor() : AccessibilityServiceBridge {

    override fun getRootNode(): AccessibilityNodeInfo? {
        return AutomatorAccessibilityService.getInstance()?.rootInActiveWindow
    }

    override fun isConnected(): StateFlow<Boolean>{
        return AutomatorAccessibilityService.isConnected
    }

    override fun performGlobalAction(action: Int): Boolean {
        return AutomatorAccessibilityService.getInstance()
            ?.performGlobalAction(action) ?: false
    }

    override fun getContext(): Context? {
        return AutomatorAccessibilityService.getInstance()
    }

    override fun dispatchGesture(gesture: GestureDescription, callback: GestureResultCallback?): Boolean {
        val service = AutomatorAccessibilityService.getInstance() ?: return false
        return service.dispatchGesture(gesture, callback, null)
    }
}